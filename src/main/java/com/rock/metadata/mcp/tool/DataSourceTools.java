package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.ConnectionTestResponse;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.service.ConnectionTestService;
import com.rock.metadata.service.TargetDataSourceManager;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataSourceTools {

    private final DataSourceConfigRepository repository;
    private final ConnectionTestService connectionTestService;
    private final TargetDataSourceManager targetDataSourceManager;

    @McpTool(description = "Register a new database datasource for metadata crawling. " +
            "Supports postgresql, mysql, oracle, sqlserver, sqlite.")
    public Map<String, Object> register_datasource(
            @McpToolParam(description = "Display name for this datasource") String name,
            @McpToolParam(description = "Database type: postgresql, mysql, oracle, sqlserver, sqlite") String dbType,
            @McpToolParam(description = "Database hostname", required = false) String host,
            @McpToolParam(description = "Database port", required = false) Integer port,
            @McpToolParam(description = "Database name", required = false) String databaseName,
            @McpToolParam(description = "Database username", required = false) String username,
            @McpToolParam(description = "Database password", required = false) String password,
            @McpToolParam(description = "JDBC URL (overrides host/port/databaseName if provided)", required = false) String jdbcUrl,
            @McpToolParam(description = "Comma-separated schema include patterns (regex)", required = false) String schemaPatterns,
            @McpToolParam(description = "Description of this datasource", required = false) String description) {
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
            return McpResponseHelper.compact(repository.save(ds));
        });
    }

    @McpTool(description = "List all registered datasources")
    public List<Map<String, Object>> list_datasources() {
        return ToolExecutor.run("list datasources", () ->
                repository.findAll().stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Get details of a specific datasource by its ID")
    public Map<String, Object> get_datasource(
            @McpToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("get datasource", () ->
                McpResponseHelper.compact(repository.findById(datasourceId)
                        .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + datasourceId))));
    }

    @McpTool(description = "Update an existing datasource configuration. Only provided fields will be updated.")
    public Map<String, Object> update_datasource(
            @McpToolParam(description = "Datasource ID to update") Long datasourceId,
            @McpToolParam(description = "Display name", required = false) String name,
            @McpToolParam(description = "Database type: postgresql, mysql, oracle, sqlserver, sqlite", required = false) String dbType,
            @McpToolParam(description = "Database hostname", required = false) String host,
            @McpToolParam(description = "Database port", required = false) Integer port,
            @McpToolParam(description = "Database name", required = false) String databaseName,
            @McpToolParam(description = "Database username", required = false) String username,
            @McpToolParam(description = "Database password", required = false) String password,
            @McpToolParam(description = "JDBC URL (overrides host/port/databaseName if provided)", required = false) String jdbcUrl,
            @McpToolParam(description = "Comma-separated schema include patterns (regex)", required = false) String schemaPatterns,
            @McpToolParam(description = "Description of this datasource", required = false) String description) {
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
            DataSourceConfig saved = repository.save(ds);
            targetDataSourceManager.evict(datasourceId);
            return McpResponseHelper.compact(saved);
        });
    }

    @McpTool(description = "Delete a datasource by its ID")
    public String delete_datasource(
            @McpToolParam(description = "Datasource ID to delete") Long datasourceId) {
        ToolExecutor.runVoid("delete datasource", () -> {
            if (!repository.existsById(datasourceId)) {
                throw new IllegalArgumentException("DataSource not found: " + datasourceId);
            }
            repository.deleteById(datasourceId);
            targetDataSourceManager.evict(datasourceId);
        });
        return "Datasource " + datasourceId + " deleted successfully";
    }

    @McpTool(description = "Test connectivity of a registered datasource. Returns success status, " +
            "response time, database product info, and error details if failed.")
    public ConnectionTestResponse test_connection(
            @McpToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("test connection", () ->
                connectionTestService.testConnection(datasourceId));
    }

    @McpTool(description = "Test connectivity with ad-hoc connection parameters (without registering a datasource)")
    public ConnectionTestResponse test_connection_adhoc(
            @McpToolParam(description = "Database type: postgresql, mysql, oracle, sqlserver, sqlite") String dbType,
            @McpToolParam(description = "Database hostname", required = false) String host,
            @McpToolParam(description = "Database port", required = false) Integer port,
            @McpToolParam(description = "Database name", required = false) String databaseName,
            @McpToolParam(description = "Database username", required = false) String username,
            @McpToolParam(description = "Database password", required = false) String password,
            @McpToolParam(description = "JDBC URL (overrides host/port/databaseName)", required = false) String jdbcUrl) {
        return ToolExecutor.run("test connection", () ->
                connectionTestService.testConnectionAdhoc(dbType, host, port, databaseName,
                        username, password, jdbcUrl));
    }
}
