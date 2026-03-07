package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.ConnectionTestResponse;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.service.ConnectionTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSourceTools {

    private final DataSourceConfigRepository repository;
    private final ConnectionTestService connectionTestService;

    @Tool(description = "Register a new database datasource for metadata crawling. " +
            "Supports postgresql, mysql, oracle, sqlserver, sqlite.")
    public DataSourceConfig register_datasource(
            @ToolParam(description = "Display name for this datasource") String name,
            @ToolParam(description = "Database type: postgresql, mysql, oracle, sqlserver, sqlite") String dbType,
            @ToolParam(description = "Database hostname", required = false) String host,
            @ToolParam(description = "Database port", required = false) Integer port,
            @ToolParam(description = "Database name", required = false) String databaseName,
            @ToolParam(description = "Database username", required = false) String username,
            @ToolParam(description = "Database password", required = false) String password,
            @ToolParam(description = "JDBC URL (overrides host/port/databaseName if provided)", required = false) String jdbcUrl,
            @ToolParam(description = "Comma-separated schema include patterns (regex)", required = false) String schemaPatterns,
            @ToolParam(description = "Description of this datasource", required = false) String description) {
        return ToolExecutor.run("register datasource", () -> {
            DataSourceConfig ds = new DataSourceConfig();
            ds.setName(name);
            ds.setDbType(dbType);
            ds.setHost(host);
            ds.setPort(port);
            ds.setDatabaseName(databaseName);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setJdbcUrl(jdbcUrl);
            ds.setSchemaPatterns(schemaPatterns);
            ds.setDescription(description);
            return repository.save(ds);
        });
    }

    @Tool(description = "List all registered datasources")
    public List<DataSourceConfig> list_datasources() {
        return ToolExecutor.run("list datasources", repository::findAll);
    }

    @Tool(description = "Get details of a specific datasource by its ID")
    public DataSourceConfig get_datasource(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("get datasource", () ->
                repository.findById(datasourceId)
                        .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + datasourceId)));
    }

    @Tool(description = "Update an existing datasource configuration. Only provided fields will be updated.")
    public DataSourceConfig update_datasource(
            @ToolParam(description = "Datasource ID to update") Long datasourceId,
            @ToolParam(description = "Display name", required = false) String name,
            @ToolParam(description = "Database type: postgresql, mysql, oracle, sqlserver, sqlite", required = false) String dbType,
            @ToolParam(description = "Database hostname", required = false) String host,
            @ToolParam(description = "Database port", required = false) Integer port,
            @ToolParam(description = "Database name", required = false) String databaseName,
            @ToolParam(description = "Database username", required = false) String username,
            @ToolParam(description = "Database password", required = false) String password,
            @ToolParam(description = "JDBC URL (overrides host/port/databaseName if provided)", required = false) String jdbcUrl,
            @ToolParam(description = "Comma-separated schema include patterns (regex)", required = false) String schemaPatterns,
            @ToolParam(description = "Description of this datasource", required = false) String description) {
        return ToolExecutor.run("update datasource", () -> {
            DataSourceConfig ds = repository.findById(datasourceId)
                    .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + datasourceId));
            if (name != null) ds.setName(name);
            if (dbType != null) ds.setDbType(dbType);
            if (host != null) ds.setHost(host);
            if (port != null) ds.setPort(port);
            if (databaseName != null) ds.setDatabaseName(databaseName);
            if (username != null) ds.setUsername(username);
            if (password != null) ds.setPassword(password);
            if (jdbcUrl != null) ds.setJdbcUrl(jdbcUrl);
            if (schemaPatterns != null) ds.setSchemaPatterns(schemaPatterns);
            if (description != null) ds.setDescription(description);
            return repository.save(ds);
        });
    }

    @Tool(description = "Delete a datasource by its ID")
    public String delete_datasource(
            @ToolParam(description = "Datasource ID to delete") Long datasourceId) {
        ToolExecutor.runVoid("delete datasource", () -> {
            if (!repository.existsById(datasourceId)) {
                throw new IllegalArgumentException("DataSource not found: " + datasourceId);
            }
            repository.deleteById(datasourceId);
        });
        return "Datasource " + datasourceId + " deleted successfully";
    }

    @Tool(description = "Test connectivity of a registered datasource. Returns success status, " +
            "response time, database product info, and error details if failed.")
    public ConnectionTestResponse test_connection(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("test connection", () ->
                connectionTestService.testConnection(datasourceId));
    }

    @Tool(description = "Test connectivity with ad-hoc connection parameters (without registering a datasource)")
    public ConnectionTestResponse test_connection_adhoc(
            @ToolParam(description = "Database type: postgresql, mysql, oracle, sqlserver, sqlite") String dbType,
            @ToolParam(description = "Database hostname", required = false) String host,
            @ToolParam(description = "Database port", required = false) Integer port,
            @ToolParam(description = "Database name", required = false) String databaseName,
            @ToolParam(description = "Database username", required = false) String username,
            @ToolParam(description = "Database password", required = false) String password,
            @ToolParam(description = "JDBC URL (overrides host/port/databaseName)", required = false) String jdbcUrl) {
        return ToolExecutor.run("test connection", () ->
                connectionTestService.testConnectionAdhoc(dbType, host, port, databaseName,
                        username, password, jdbcUrl));
    }
}
