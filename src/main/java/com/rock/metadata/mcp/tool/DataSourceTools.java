package com.rock.metadata.mcp.tool;

import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.DataSourceConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSourceTools {

    private final DataSourceConfigRepository repository;

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
    }

    @Tool(description = "List all registered datasources")
    public List<DataSourceConfig> list_datasources() {
        return repository.findAll();
    }

    @Tool(description = "Get details of a specific datasource by its ID")
    public DataSourceConfig get_datasource(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return repository.findById(datasourceId)
                .orElseThrow(() -> new RuntimeException("DataSource not found: " + datasourceId));
    }

    @Tool(description = "Update an existing datasource configuration")
    public DataSourceConfig update_datasource(
            @ToolParam(description = "Datasource ID to update") Long datasourceId,
            @ToolParam(description = "Display name") String name,
            @ToolParam(description = "Database type: postgresql, mysql, oracle, sqlserver, sqlite") String dbType,
            @ToolParam(description = "Database hostname", required = false) String host,
            @ToolParam(description = "Database port", required = false) Integer port,
            @ToolParam(description = "Database name", required = false) String databaseName,
            @ToolParam(description = "Database username", required = false) String username,
            @ToolParam(description = "Database password", required = false) String password,
            @ToolParam(description = "JDBC URL (overrides host/port/databaseName if provided)", required = false) String jdbcUrl,
            @ToolParam(description = "Comma-separated schema include patterns (regex)", required = false) String schemaPatterns,
            @ToolParam(description = "Description of this datasource", required = false) String description) {

        DataSourceConfig ds = repository.findById(datasourceId)
                .orElseThrow(() -> new RuntimeException("DataSource not found: " + datasourceId));
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
    }

    @Tool(description = "Delete a datasource by its ID")
    public String delete_datasource(
            @ToolParam(description = "Datasource ID to delete") Long datasourceId) {
        if (!repository.existsById(datasourceId)) {
            throw new RuntimeException("DataSource not found: " + datasourceId);
        }
        repository.deleteById(datasourceId);
        return "Datasource " + datasourceId + " deleted successfully";
    }
}
