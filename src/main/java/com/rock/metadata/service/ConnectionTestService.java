package com.rock.metadata.service;

import com.rock.metadata.dto.ConnectionTestResponse;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.DataSourceConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionTestService {

    private static final int TIMEOUT_SECONDS = 5;

    private final DataSourceConfigRepository dataSourceConfigRepository;

    public ConnectionTestResponse testConnection(Long datasourceId) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));
        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        return doTest(jdbcUrl, ds.getUsername(), ds.getPassword());
    }

    public ConnectionTestResponse testConnectionAdhoc(String dbType, String host, Integer port,
                                                       String databaseName, String username,
                                                       String password, String jdbcUrl) {
        String url;
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            url = jdbcUrl;
        } else {
            DataSourceConfig temp = new DataSourceConfig();
            temp.setDbType(dbType);
            temp.setHost(host);
            temp.setPort(port);
            temp.setDatabaseName(databaseName);
            url = JdbcUrlBuilder.buildJdbcUrl(temp);
        }
        return doTest(url, username, password);
    }

    private ConnectionTestResponse doTest(String jdbcUrl, String username, String password) {
        ConnectionTestResponse response = new ConnectionTestResponse();
        long start = System.currentTimeMillis();

        try {
            DriverManager.setLoginTimeout(TIMEOUT_SECONDS);
            Properties props = new Properties();
            if (username != null) props.setProperty("user", username);
            if (password != null) props.setProperty("password", password);

            try (Connection conn = DriverManager.getConnection(jdbcUrl, props)) {
                conn.setNetworkTimeout(Runnable::run, TIMEOUT_SECONDS * 1000);
                DatabaseMetaData meta = conn.getMetaData();
                response.setSuccess(true);
                response.setDatabaseProductName(meta.getDatabaseProductName());
                response.setDatabaseProductVersion(meta.getDatabaseProductVersion());
                response.setDriverName(meta.getDriverName());
            }
        } catch (SQLException e) {
            log.warn("Connection test failed for {}: {}", jdbcUrl, e.getMessage());
            response.setSuccess(false);
            response.setErrorMessage(e.getMessage());
        }

        response.setResponseTimeMs(System.currentTimeMillis() - start);
        return response;
    }
}
