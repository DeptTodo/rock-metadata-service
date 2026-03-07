package com.rock.metadata.service;

import com.rock.metadata.dto.ConnectionTestResponse;
import com.rock.metadata.dto.MetadataHealthResponse;
import com.rock.metadata.model.CrawlJob;
import com.rock.metadata.model.CrawlStatus;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.repository.CrawlJobRepository;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.repository.MetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetadataHealthService {

    private final CrawlJobRepository crawlJobRepository;
    private final MetaTableRepository metaTableRepository;
    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final ConnectionTestService connectionTestService;

    public MetadataHealthResponse checkHealth(Long datasourceId) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));

        MetadataHealthResponse response = new MetadataHealthResponse();
        List<String> warnings = new ArrayList<>();

        // Freshness
        Optional<CrawlJob> latestJob = crawlJobRepository
                .findFirstByDatasourceIdAndStatusOrderByFinishedAtDesc(datasourceId, CrawlStatus.SUCCESS);

        if (latestJob.isEmpty()) {
            response.setFreshnessStatus("NO_DATA");
            response.setCrawledTableCount(0);
            warnings.add("No successful crawl found for this datasource");
        } else {
            CrawlJob job = latestJob.get();
            response.setLastCrawlTime(job.getFinishedAt());
            List<MetaTable> tables = metaTableRepository.findByCrawlJobId(job.getId());
            response.setCrawledTableCount(tables.size());

            Duration age = Duration.between(job.getFinishedAt(), LocalDateTime.now());
            if (age.toHours() < 24) {
                response.setFreshnessStatus("FRESH");
            } else if (age.toDays() < 7) {
                response.setFreshnessStatus("AGING");
                warnings.add("Crawl data is " + age.toDays() + " days old");
            } else {
                response.setFreshnessStatus("STALE");
                warnings.add("Crawl data is " + age.toDays() + " days old — consider re-crawling");
            }
        }

        // Connection test
        ConnectionTestResponse connTest = connectionTestService.testConnection(datasourceId);
        response.setConnectionReachable(connTest.isSuccess());
        if (!connTest.isSuccess()) {
            warnings.add("Connection failed: " + connTest.getErrorMessage());
        }

        // Live table count comparison
        if (connTest.isSuccess()) {
            Integer liveCount = countLiveTables(ds);
            response.setLiveTableCount(liveCount);
            if (liveCount != null && latestJob.isPresent()) {
                int diff = Math.abs(liveCount - response.getCrawledTableCount());
                if (diff > 0) {
                    warnings.add("Table count mismatch: crawled=" + response.getCrawledTableCount()
                            + " vs live=" + liveCount);
                }
            }
        }

        // Overall health
        if ("NO_DATA".equals(response.getFreshnessStatus()) || !response.isConnectionReachable()) {
            response.setOverallHealth("UNHEALTHY");
        } else if (!warnings.isEmpty()) {
            response.setOverallHealth("WARNING");
        } else {
            response.setOverallHealth("HEALTHY");
        }
        response.setWarnings(warnings);

        return response;
    }

    private Integer countLiveTables(DataSourceConfig ds) {
        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), ds.getPassword())) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
                int count = 0;
                while (rs.next()) count++;
                return count;
            }
        } catch (SQLException e) {
            log.warn("Failed to count live tables for datasource {}: {}", ds.getId(), e.getMessage());
            return null;
        }
    }
}
