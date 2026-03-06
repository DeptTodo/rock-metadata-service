package com.rock.metadata.service;

import com.rock.metadata.model.DataSourceConfig;

public final class JdbcUrlBuilder {

    private JdbcUrlBuilder() {}

    public static String buildJdbcUrl(DataSourceConfig ds) {
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
