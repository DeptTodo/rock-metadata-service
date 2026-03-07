package com.rock.metadata.service;

import com.rock.metadata.dto.SqlExecuteResponse;
import com.rock.metadata.dto.TableRowCount;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.repository.MetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecuteService {

    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final MetaTableRepository metaTableRepository;
    private final TargetDataSourceManager targetDataSourceManager;

    private static final int MAX_ROWS = 10000;
    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final int COUNT_PARALLELISM = 20;
    private static final int COUNT_TIMEOUT_SECONDS = 15;

    public SqlExecuteResponse execute(Long datasourceId, String sql) {
        return execute(datasourceId, sql, MAX_ROWS);
    }

    public SqlExecuteResponse execute(Long datasourceId, String sql, int maxRows) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));

        int effectiveMaxRows = Math.min(Math.max(maxRows, 1), MAX_ROWS);
        log.info("Executing SQL on datasource {}: {}", datasourceId,
                sql == null ? "null" : sql.substring(0, Math.min(sql.length(), 200)));

        try (Connection conn = targetDataSourceManager.getConnection(ds);
             Statement stmt = conn.createStatement()) {

            // Fetch one extra row to detect truncation
            stmt.setMaxRows(effectiveMaxRows + 1);
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            boolean isQuery = stmt.execute(sql);
            SqlExecuteResponse response = new SqlExecuteResponse();
            response.setQuery(isQuery);

            if (isQuery) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }
                    response.setColumns(columns);

                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(columns.get(i - 1), rs.getObject(i));
                        }
                        rows.add(row);
                    }

                    // Check if we got more rows than the limit (truncation indicator)
                    if (rows.size() > effectiveMaxRows) {
                        rows = rows.subList(0, effectiveMaxRows);
                        response.setTruncated(true);
                    }
                    response.setReturnedRows(rows.size());
                    response.setRows(rows);
                }
            } else {
                response.setAffectedRows(stmt.getUpdateCount());
            }

            return response;

        } catch (SQLException e) {
            log.error("SQL execution failed on datasource {}", datasourceId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "SQL execution failed: " + e.getSQLState());
        }
    }

    /**
     * Count rows for all given tables using parallel execution.
     * Results are persisted to MetaTable.rowCount for future use.
     */
    public List<TableRowCount> countTableRows(Long datasourceId, List<MetaTable> tables) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));

        if (tables.isEmpty()) {
            return List.of();
        }

        String dbType = ds.getDbType().toLowerCase();
        int parallelism = Math.min(COUNT_PARALLELISM, tables.size());
        ExecutorService executor = Executors.newFixedThreadPool(parallelism,
                r -> { Thread t = new Thread(r, "row-count"); t.setDaemon(true); return t; });

        try {
            List<CompletableFuture<TableRowCount>> futures = tables.stream()
                    .map(table -> CompletableFuture.supplyAsync(() -> countSingleTable(ds, dbType, table), executor))
                    .toList();

            // Wait for all to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            List<TableRowCount> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            // Persist row counts to MetaTable in batch and set timestamps on DTOs
            LocalDateTime now = LocalDateTime.now();
            persistRowCounts(tables, results, now);
            for (TableRowCount rc : results) {
                if (rc.getRowCount() != null && rc.getError() == null) {
                    rc.setRowCountUpdatedAt(now);
                }
            }

            return results;
        } finally {
            executor.shutdown();
        }
    }

    private TableRowCount countSingleTable(DataSourceConfig ds, String dbType, MetaTable table) {
        TableRowCount rc = new TableRowCount();
        rc.setTableId(table.getId());
        rc.setSchemaName(table.getSchemaName());
        rc.setTableName(table.getTableName());
        rc.setFullName(table.getFullName());

        String countSql = buildCountSql(dbType, table.getSchemaName(), table.getTableName());
        try (Connection conn = targetDataSourceManager.getConnection(ds);
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(COUNT_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next()) {
                    rc.setRowCount(rs.getLong(1));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to count rows for table {}: {}", table.getFullName(), e.getMessage());
            rc.setError(e.getMessage());
        }
        return rc;
    }

    private void persistRowCounts(List<MetaTable> tables, List<TableRowCount> results, LocalDateTime now) {
        try {
            Map<Long, Long> countMap = new HashMap<>();
            for (TableRowCount rc : results) {
                if (rc.getRowCount() != null && rc.getError() == null) {
                    countMap.put(rc.getTableId(), rc.getRowCount());
                }
            }
            if (countMap.isEmpty()) return;

            List<MetaTable> toSave = new ArrayList<>();
            for (MetaTable table : tables) {
                Long count = countMap.get(table.getId());
                if (count != null) {
                    table.setRowCount(count);
                    table.setRowCountUpdatedAt(now);
                    toSave.add(table);
                }
            }
            metaTableRepository.saveAll(toSave);
            log.info("Persisted row counts for {} tables", toSave.size());
        } catch (Exception e) {
            log.warn("Failed to persist row counts: {}", e.getMessage());
        }
    }

    private String buildCountSql(String dbType, String schemaName, String tableName) {
        return "SELECT COUNT(*) FROM " + JdbcUrlBuilder.qualifyTable(dbType, schemaName, tableName);
    }

}
