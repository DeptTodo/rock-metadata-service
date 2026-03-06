package com.rock.metadata.service;

import com.rock.metadata.dto.SqlExecuteResponse;
import com.rock.metadata.model.DataSourceConfig;
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

    public SqlExecuteResponse execute(Long datasourceId, String sql) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));

        String jdbcUrl = buildJdbcUrl(ds);
        log.info("Executing SQL on datasource {}: {}", datasourceId, sql);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), ds.getPassword());
             Statement stmt = conn.createStatement()) {

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private String buildJdbcUrl(DataSourceConfig ds) {
        if (ds.getJdbcUrl() != null && !ds.getJdbcUrl().isBlank()) {
            return ds.getJdbcUrl();
        }
        String host = ds.getHost() != null ? ds.getHost() : "localhost";
        return switch (ds.getDbType().toLowerCase()) {
            case "postgresql", "postgres" -> {
                int port = ds.getPort() != null ? ds.getPort() : 5432;
                yield "jdbc:postgresql://%s:%d/%s".formatted(host, port, ds.getDatabaseName());
            }
            case "mysql" -> {
                int port = ds.getPort() != null ? ds.getPort() : 3306;
                yield "jdbc:mysql://%s:%d/%s".formatted(host, port, ds.getDatabaseName());
            }
            case "oracle" -> {
                int port = ds.getPort() != null ? ds.getPort() : 1521;
                yield "jdbc:oracle:thin:@%s:%d/%s".formatted(host, port, ds.getDatabaseName());
            }
            case "sqlserver" -> {
                int port = ds.getPort() != null ? ds.getPort() : 1433;
                yield "jdbc:sqlserver://%s:%d;databaseName=%s;trustServerCertificate=true"
                        .formatted(host, port, ds.getDatabaseName());
            }
            case "sqlite" -> "jdbc:sqlite:%s".formatted(ds.getDatabaseName());
            default -> throw new IllegalArgumentException("Unsupported database type: " + ds.getDbType());
        };
    }
}
