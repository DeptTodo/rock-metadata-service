package com.rock.metadata.service;

import com.rock.metadata.dto.*;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.model.MetaColumn;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.repository.MetaColumnRepository;
import com.rock.metadata.repository.MetaTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataProfilingService {

    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final int SAMPLE_LIMIT = 5;
    private static final int DEFAULT_ROW_SAMPLE_LIMIT = 10;
    private static final int MAX_ROW_SAMPLE_LIMIT = 100;
    private static final int DEFAULT_DISTINCT_LIMIT = 50;
    private static final int MAX_DISTINCT_LIMIT = 500;

    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final TargetDataSourceManager targetDataSourceManager;

    public TableProfileResponse profileTable(Long datasourceId, Long tableId, List<String> columns) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));
        MetaTable table = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

        List<MetaColumn> metaColumns = metaColumnRepository.findByTableIdOrderByOrdinalPosition(tableId);
        if (columns != null && !columns.isEmpty()) {
            metaColumns = metaColumns.stream()
                    .filter(c -> columns.stream().anyMatch(cn -> cn.equalsIgnoreCase(c.getColumnName())))
                    .toList();
        }

        String dbType = ds.getDbType().toLowerCase();


        TableProfileResponse response = new TableProfileResponse();
        response.setTableId(table.getId());
        response.setTableName(table.getTableName());
        response.setFullName(table.getFullName());

        try (Connection conn = targetDataSourceManager.getConnection(ds)) {
            // Row count
            response.setRowCount(getRowCount(conn, dbType, table));

            // Column profiles
            List<ColumnProfile> profiles = new ArrayList<>();
            for (MetaColumn col : metaColumns) {
                profiles.add(profileColumn(conn, dbType, table, col));
            }
            response.setColumnProfiles(profiles);
        } catch (SQLException e) {
            log.error("Profiling failed for table {}: {}", table.getFullName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Profiling failed: " + e.getMessage());
        }

        return response;
    }

    public ColumnProfile profileSingleColumn(Long datasourceId, Long tableId, String columnName) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));
        MetaTable table = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

        List<MetaColumn> metaColumns = metaColumnRepository.findByTableIdOrderByOrdinalPosition(tableId);
        MetaColumn col = metaColumns.stream()
                .filter(c -> c.getColumnName().equalsIgnoreCase(columnName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Column not found: " + columnName));

        String dbType = ds.getDbType().toLowerCase();


        try (Connection conn = targetDataSourceManager.getConnection(ds)) {
            return profileColumn(conn, dbType, table, col);
        } catch (SQLException e) {
            log.error("Profiling failed for column {}.{}: {}", table.getFullName(), columnName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Profiling failed: " + e.getMessage());
        }
    }

    public DataSampleResponse sampleTableRows(Long datasourceId, Long tableId, Integer limit) {
        int effectiveLimit = limit != null ? Math.min(limit, MAX_ROW_SAMPLE_LIMIT) : DEFAULT_ROW_SAMPLE_LIMIT;

        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));
        MetaTable table = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

        String dbType = ds.getDbType().toLowerCase();

        String qualifiedTable = qualifyTable(dbType, table);

        DataSampleResponse response = new DataSampleResponse();
        response.setTableId(table.getId());
        response.setTableName(table.getTableName());
        response.setFullName(table.getFullName());
        response.setLimit(effectiveLimit);

        try (Connection conn = targetDataSourceManager.getConnection(ds)) {
            response.setTotalRowCount(getRowCount(conn, dbType, table));

            String sql = buildLimitSql(dbType, "SELECT * FROM " + qualifiedTable, effectiveLimit);
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    List<String> columnNames = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        columnNames.add(meta.getColumnName(i));
                    }
                    response.setColumnNames(columnNames);

                    List<Map<String, Object>> rows = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= colCount; i++) {
                            Object val = rs.getObject(i);
                            row.put(columnNames.get(i - 1), val != null ? val.toString() : null);
                        }
                        rows.add(row);
                    }
                    response.setRows(rows);
                }
            }
        } catch (SQLException e) {
            log.error("Sample rows failed for table {}: {}", table.getFullName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sample rows failed: " + e.getMessage());
        }

        return response;
    }

    public DistinctValueResponse getDistinctColumnValues(Long datasourceId, Long tableId,
                                                          String columnName, Integer limit) {
        int effectiveLimit = limit != null ? Math.min(limit, MAX_DISTINCT_LIMIT) : DEFAULT_DISTINCT_LIMIT;

        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));
        MetaTable table = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

        List<MetaColumn> metaColumns = metaColumnRepository.findByTableIdOrderByOrdinalPosition(tableId);
        MetaColumn col = metaColumns.stream()
                .filter(c -> c.getColumnName().equalsIgnoreCase(columnName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Column not found: " + columnName));

        String dbType = ds.getDbType().toLowerCase();

        String quotedCol = quoteIdentifier(dbType, col.getColumnName());
        String qualifiedTable = qualifyTable(dbType, table);

        DistinctValueResponse response = new DistinctValueResponse();
        response.setTableId(table.getId());
        response.setTableName(table.getTableName());
        response.setColumnName(col.getColumnName());
        response.setDataType(col.getDataType());

        try (Connection conn = targetDataSourceManager.getConnection(ds)) {
            // Total distinct count
            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                String countSql = String.format("SELECT COUNT(DISTINCT %s) FROM %s", quotedCol, qualifiedTable);
                try (ResultSet rs = stmt.executeQuery(countSql)) {
                    if (rs.next()) {
                        response.setTotalDistinctCount(rs.getLong(1));
                    }
                }
            }

            // Distinct values with counts, ordered by frequency desc
            String innerSql = String.format(
                    "SELECT %s, COUNT(*) AS cnt FROM %s WHERE %s IS NOT NULL GROUP BY %s ORDER BY cnt DESC",
                    quotedCol, qualifiedTable, quotedCol, quotedCol);
            String sql = buildLimitSql(dbType, innerSql, effectiveLimit);

            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                List<DistinctValueResponse.ValueCount> values = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        DistinctValueResponse.ValueCount vc = new DistinctValueResponse.ValueCount();
                        Object val = rs.getObject(1);
                        vc.setValue(val != null ? val.toString() : null);
                        vc.setCount(rs.getLong(2));
                        values.add(vc);
                    }
                }
                response.setValues(values);
            }
        } catch (SQLException e) {
            log.error("Distinct values query failed for {}.{}: {}", table.getFullName(), columnName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Distinct values query failed: " + e.getMessage());
        }

        return response;
    }

    private long getRowCount(Connection conn, String dbType, MetaTable table) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + qualifyTable(dbType, table);
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private ColumnProfile profileColumn(Connection conn, String dbType,
                                         MetaTable table, MetaColumn col) throws SQLException {
        ColumnProfile profile = new ColumnProfile();
        profile.setColumnName(col.getColumnName());
        profile.setDataType(col.getDataType());

        String quotedCol = quoteIdentifier(dbType, col.getColumnName());
        String qualifiedTable = qualifyTable(dbType, table);

        // Aggregate query: distinct count, null count, min, max
        String sql = String.format(
                "SELECT COUNT(DISTINCT %s), SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END), " +
                "COUNT(*), MIN(CAST(%s AS VARCHAR(255))), MAX(CAST(%s AS VARCHAR(255))) FROM %s",
                quotedCol, quotedCol, quotedCol, quotedCol, qualifiedTable);

        // Use database-specific CAST for non-string-safe types
        String safeSql = String.format(
                "SELECT COUNT(DISTINCT %s), SUM(CASE WHEN %s IS NULL THEN 1 ELSE 0 END), " +
                "COUNT(*) FROM %s",
                quotedCol, quotedCol, qualifiedTable);

        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

            try (ResultSet rs = stmt.executeQuery(safeSql)) {
                if (rs.next()) {
                    profile.setDistinctCount(rs.getLong(1));
                    profile.setNullCount(rs.getLong(2));
                    long total = rs.getLong(3);
                    profile.setNullPercentage(total > 0 ? (double) profile.getNullCount() / total * 100 : 0);
                }
            }
        }

        // Min/Max via separate query to handle type issues gracefully
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            String minMaxSql = String.format("SELECT MIN(%s), MAX(%s) FROM %s",
                    quotedCol, quotedCol, qualifiedTable);
            try (ResultSet rs = stmt.executeQuery(minMaxSql)) {
                if (rs.next()) {
                    Object min = rs.getObject(1);
                    Object max = rs.getObject(2);
                    profile.setMinValue(min != null ? min.toString() : null);
                    profile.setMaxValue(max != null ? max.toString() : null);
                }
            }
        } catch (SQLException e) {
            log.debug("Min/Max query failed for {}.{}: {}", table.getFullName(), col.getColumnName(), e.getMessage());
        }

        // Sample values
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            String sampleSql = buildSampleSql(dbType, qualifiedTable, quotedCol, SAMPLE_LIMIT);
            List<String> samples = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(sampleSql)) {
                while (rs.next()) {
                    Object val = rs.getObject(1);
                    if (val != null) samples.add(val.toString());
                }
            }
            profile.setSampleValues(samples);
        } catch (SQLException e) {
            log.debug("Sample query failed for {}.{}: {}", table.getFullName(), col.getColumnName(), e.getMessage());
            profile.setSampleValues(List.of());
        }

        return profile;
    }

    private String qualifyTable(String dbType, MetaTable table) {
        return JdbcUrlBuilder.qualifyTable(dbType, table.getSchemaName(), table.getTableName());
    }

    private String quoteIdentifier(String dbType, String identifier) {
        return JdbcUrlBuilder.quoteIdentifier(dbType, identifier);
    }

    private String buildSampleSql(String dbType, String table, String column, int limit) {
        return switch (dbType) {
            case "sqlserver" -> String.format("SELECT TOP %d %s FROM %s WHERE %s IS NOT NULL",
                    limit, column, table, column);
            case "oracle" -> String.format("SELECT %s FROM %s WHERE %s IS NOT NULL AND ROWNUM <= %d",
                    column, table, column, limit);
            default -> String.format("SELECT %s FROM %s WHERE %s IS NOT NULL LIMIT %d",
                    column, table, column, limit);
        };
    }

    private String buildLimitSql(String dbType, String baseSql, int limit) {
        return switch (dbType) {
            case "sqlserver" -> baseSql.replaceFirst("(?i)^SELECT ", "SELECT TOP " + limit + " ");
            case "oracle" -> "SELECT * FROM (" + baseSql + ") WHERE ROWNUM <= " + limit;
            default -> baseSql + " LIMIT " + limit;
        };
    }
}
