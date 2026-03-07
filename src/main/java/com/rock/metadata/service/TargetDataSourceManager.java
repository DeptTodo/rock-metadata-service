package com.rock.metadata.service;

import com.rock.metadata.model.DataSourceConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pooled JDBC connections to target (crawled) datasources.
 * Caches a small HikariCP pool per datasourceId, auto-evicts on config changes.
 */
@Slf4j
@Service
public class TargetDataSourceManager {

    private static final int POOL_SIZE = 3;
    private static final int MIN_IDLE = 1;
    private static final long IDLE_TIMEOUT_MS = 120_000;    // 2 min
    private static final long MAX_LIFETIME_MS = 600_000;     // 10 min
    private static final long CONNECTION_TIMEOUT_MS = 5_000; // 5 sec

    private final ConcurrentHashMap<Long, HikariDataSource> pools = new ConcurrentHashMap<>();

    public Connection getConnection(DataSourceConfig ds) throws SQLException {
        HikariDataSource pool = pools.computeIfAbsent(ds.getId(), id -> createPool(ds));
        return pool.getConnection();
    }

    public void evict(Long datasourceId) {
        HikariDataSource pool = pools.remove(datasourceId);
        if (pool != null) {
            log.info("Evicting connection pool for datasource {}", datasourceId);
            pool.close();
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
        config.setPoolName("target-ds-" + ds.getId());
        config.setReadOnly(false);
        config.setAutoCommit(true);

        return new HikariDataSource(config);
    }
}
