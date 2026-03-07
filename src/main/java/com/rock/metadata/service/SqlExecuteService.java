package com.rock.metadata.service;

import com.rock.metadata.dto.SqlExecuteResponse;
import com.rock.metadata.dto.TableRowCount;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.repository.DataSourceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecuteService {

    private final DataSourceConfigRepository dataSourceConfigRepository;

    private static final int MAX_ROWS = 10000;

    private static final int QUERY_TIMEOUT_SECONDS = 30;

    public SqlExecuteResponse execute(Long datasourceId, String sql) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));

        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        log.info("Executing SQL on datasource {}: {}", datasourceId,
                sql == null ? "null" : sql.substring(0, Math.min(sql.length(), 200)));

        try (Connection conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), ds.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.setMaxRows(MAX_ROWS);
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

    public List<TableRowCount> countTableRows(Long datasourceId, List<MetaTable> tables) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));

        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        String dbType = ds.getDbType().toLowerCase();
        List<TableRowCount> results = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), ds.getPassword());
             Statement stmt = conn.createStatement()) {

            for (MetaTable table : tables) {
                TableRowCount rc = new TableRowCount();
                rc.setTableId(table.getId());
                rc.setSchemaName(table.getSchemaName());
                rc.setTableName(table.getTableName());
                rc.setFullName(table.getFullName());

                String countSql = buildCountSql(dbType, table.getSchemaName(), table.getTableName());
                try (ResultSet rs = stmt.executeQuery(countSql)) {
                    if (rs.next()) {
                        rc.setRowCount(rs.getLong(1));
                    }
                } catch (SQLException e) {
                    log.warn("Failed to count rows for table {}: {}", table.getFullName(), e.getMessage());
                    rc.setError(e.getMessage());
                }
                results.add(rc);
            }
        } catch (SQLException e) {
            log.error("Failed to connect to datasource {}", datasourceId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to connect: " + e.getMessage());
        }

        return results;
    }

    private String buildCountSql(String dbType, String schemaName, String tableName) {
        return "SELECT COUNT(*) FROM " + JdbcUrlBuilder.qualifyTable(dbType, schemaName, tableName);
    }

}
