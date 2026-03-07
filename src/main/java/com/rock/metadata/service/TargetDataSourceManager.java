package com.rock.metadata.service;

import com.rock.metadata.model.DataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pooled JDBC connections to target (crawled) datasources.
 * Caches a small HikariCP pool per datasourceId, auto-evicts on config changes.
 * Idle pools with no active connections are automatically cleaned up.
 */
@Slf4j
@Service
public class TargetDataSourceManager {

    private static final int POOL_SIZE = 5;
    private static final int MIN_IDLE = 1;
    private static final long IDLE_TIMEOUT_MS = 60_000;          // 1 min — idle connections shrink quickly
    private static final long MAX_LIFETIME_MS = 300_000;          // 5 min — recycle before DB-side timeouts
    private static final long CONNECTION_TIMEOUT_MS = 10_000;     // 10 sec — generous for remote DBs
    private static final long KEEPALIVE_TIME_MS = 60_000;         // 1 min — proactively validate idle connections
    private static final long VALIDATION_TIMEOUT_MS = 3_000;      // 3 sec — isValid() timeout
    private static final long LEAK_DETECTION_MS = 30_000;         // 30 sec — detect unreturned connections
    private static final long POOL_UNUSED_EVICT_MS = 600_000;     // 10 min — evict pools with no recent usage

    private final ConcurrentHashMap<Long, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> lastAccessTime = new ConcurrentHashMap<>();

    public Connection getConnection(DataSourceConfig ds) throws SQLException {
        lastAccessTime.put(ds.getId(), System.currentTimeMillis());
        HikariDataSource pool = pools.computeIfAbsent(ds.getId(), id -> createPool(ds));
        return pool.getConnection();
    }

    public void evict(Long datasourceId) {
        lastAccessTime.remove(datasourceId);
        HikariDataSource pool = pools.remove(datasourceId);
        if (pool != null) {
            log.info("Evicting connection pool for datasource {}", datasourceId);
            pool.close();
        }
    }

    /**
     * Periodically evict pools that have had no activity for POOL_UNUSED_EVICT_MS.
     * Runs every 2 minutes.
     */
    @Scheduled(fixedDelay = 120_000, initialDelay = 120_000)
    public void evictIdlePools() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Long, HikariDataSource>> it = pools.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, HikariDataSource> entry = it.next();
            Long dsId = entry.getKey();
            HikariDataSource pool = entry.getValue();

            // Skip pools that still have active (in-use) connections
            if (pool.getHikariPoolMXBean() != null
                    && pool.getHikariPoolMXBean().getActiveConnections() > 0) {
                continue;
            }

            Long lastAccess = lastAccessTime.getOrDefault(dsId, 0L);
            if (now - lastAccess > POOL_UNUSED_EVICT_MS) {
                it.remove();
                lastAccessTime.remove(dsId);
                log.info("Auto-evicting idle connection pool for datasource {} (unused for {}s)",
                        dsId, (now - lastAccess) / 1000);
                try {
                    pool.close();
                } catch (Exception e) {
                    log.warn("Error closing idle pool for datasource {}", dsId, e);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down {} target connection pool(s)", pools.size());
        pools.forEach((id, pool) -> {
            try {
                pool.close();
            } catch (Exception e) {
                log.warn("Error closing pool for datasource {}", id, e);
            }
        });
        pools.clear();
        lastAccessTime.clear();
    }

    private HikariDataSource createPool(DataSourceConfig ds) {
        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        log.info("Creating connection pool for datasource {} ({})", ds.getId(), ds.getName());

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(ds.getUsername());
        config.setPassword(ds.getPassword());
        config.setMaximumPoolSize(POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setIdleTimeout(IDLE_TIMEOUT_MS);
        config.setMaxLifetime(MAX_LIFETIME_MS);
        config.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        config.setKeepaliveTime(KEEPALIVE_TIME_MS);
        config.setValidationTimeout(VALIDATION_TIMEOUT_MS);
        config.setLeakDetectionThreshold(LEAK_DETECTION_MS);
        config.setPoolName("target-ds-" + ds.getId());
        config.setReadOnly(false);
        config.setAutoCommit(true);

        return new HikariDataSource(config);
    }
}
