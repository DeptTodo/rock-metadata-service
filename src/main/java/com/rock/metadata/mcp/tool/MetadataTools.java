package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.*;
import com.rock.metadata.model.*;
import com.rock.metadata.service.DatasourceSummaryService;
import com.rock.metadata.service.MetadataExportService;
import com.rock.metadata.service.MetadataHealthService;
import com.rock.metadata.service.MetadataQueryService;
import com.rock.metadata.service.SqlExecuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.rock.metadata.mcp.tool.McpResponseHelper.*;

@Component
@RequiredArgsConstructor
public class MetadataTools {

    private final MetadataQueryService metadataQueryService;
    private final SqlExecuteService sqlExecuteService;
    private final MetadataExportService metadataExportService;
    private final DatasourceSummaryService datasourceSummaryService;
    private final MetadataHealthService metadataHealthService;

    @Tool(description = "List all schemas from the latest successful crawl of a datasource. " +
            "Returns compact keys: id, catalog, schema, full, remarks, display, bizDesc, owner.")
    public List<Map<String, Object>> list_schemas(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("list schemas", () ->
                metadataQueryService.listSchemas(datasourceId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @Tool(description = "List tables from the latest successful crawl. Returns compact summaries " +
            "(id, schema, name, full, type, rowCount, comment, display, domain, importance). " +
            "Row counts are auto-populated after crawl. Use sortByRowCount=true to get tables ordered by data volume descending. " +
            "Use get_table_detail for full information on a specific table.")
    public List<Map<String, Object>> list_tables(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "If true, only return tables not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly,
            @ToolParam(description = "If true, sort tables by row count descending (optional)", required = false) Boolean sortByRowCount) {
        return ToolExecutor.run("list tables", () ->
                tableSummaries(metadataQueryService.listTables(datasourceId, schema, unanalyzedOnly,
                        Boolean.TRUE.equals(sortByRowCount))));
    }

    @Tool(description = "Get full table detail with compact keys. Includes table info, columns (summary), " +
            "pks, fks, indexes, triggers, constraints. Use export_metadata for complete DDL.")
    public Map<String, Object> get_table_detail(
            @ToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("get table detail", () ->
                compactTableDetail(metadataQueryService.getTableDetail(tableId)));
    }

    @Tool(description = "List all columns of a table ordered by ordinal position. Returns compact summaries " +
            "(id, tblId, name, pos, type, size, nullable, pk, fk, comment, display, sensitivity). " +
            "Use get_table_detail for full column information.")
    public List<Map<String, Object>> list_columns(
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "If true, only return columns not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly) {
        return ToolExecutor.run("list columns", () ->
                columnSummaries(metadataQueryService.listColumns(tableId, unanalyzedOnly)));
    }

    @Tool(description = "List foreign keys of a table with compact keys: id, name, fkCol, pkTable, pkCol, onUpdate, onDelete")
    public List<Map<String, Object>> list_foreign_keys(
            @ToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("list foreign keys", () ->
                metadataQueryService.listForeignKeys(tableId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @Tool(description = "List indexes of a table with compact keys: id, name, col, pos, type, unique, sort")
    public List<Map<String, Object>> list_indexes(
            @ToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("list indexes", () ->
                metadataQueryService.listIndexes(tableId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @Tool(description = "Get row counts for tables. By default returns pre-stored counts from metadata " +
            "(auto-populated after crawl). Set refresh=true to re-count by querying the target datasource live. " +
            "Optionally filter by schema name or table name. Results are sorted by row count descending.")
    public List<TableRowCount> count_table_rows(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "Table name to filter by (optional)", required = false) String tableName,
            @ToolParam(description = "If true, re-count by querying the target database live instead of using stored counts (optional, default false)", required = false) Boolean refresh) {
        return ToolExecutor.run("count table rows", () -> {
            List<MetaTable> tables = metadataQueryService.listTables(datasourceId, schema, null, true);
            if (tableName != null && !tableName.isBlank()) {
                tables = tables.stream()
                        .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                        .toList();
            }
            if (Boolean.TRUE.equals(refresh)) {
                return sqlExecuteService.countTableRows(datasourceId, tables);
            }
            // Return pre-stored counts from MetaTable
            return tables.stream().map(t -> {
                TableRowCount rc = new TableRowCount();
                rc.setTableId(t.getId());
                rc.setSchemaName(t.getSchemaName());
                rc.setTableName(t.getTableName());
                rc.setFullName(t.getFullName());
                rc.setRowCount(t.getRowCount());
                rc.setRowCountUpdatedAt(t.getRowCountUpdatedAt());
                return rc;
            }).toList();
        });
    }

    @Tool(description = "Search tables and columns by keyword across a datasource's latest crawl. " +
            "Returns compact summaries, capped at " + MAX_SEARCH_TABLES + " tables and " + MAX_SEARCH_COLUMNS + " columns.")
    public Map<String, Object> search_metadata(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Search keyword") String keyword) {
        return ToolExecutor.run("search metadata", () ->
                compactSearchResult(metadataQueryService.search(datasourceId, keyword)));
    }

    // ===== Routines =====

    @Tool(description = "List stored procedures and functions. Returns compact summaries " +
            "(id, schema, name, full, type, returnType, remarks). " +
            "Use get_routine_detail for full definition source code.")
    public List<Map<String, Object>> list_routines(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return ToolExecutor.run("list routines", () ->
                routineSummaries(metadataQueryService.listRoutines(datasourceId, schema)));
    }

    @Tool(description = "Get routine detail including parameter list and full definition source code")
    public Map<String, Object> get_routine_detail(
            @ToolParam(description = "Routine ID") Long routineId) {
        return ToolExecutor.run("get routine detail", () ->
                compactRoutineDetail(metadataQueryService.getRoutineDetail(routineId)));
    }

    // ===== Sequences =====

    @Tool(description = "List sequences from the latest successful crawl. Returns compact summaries " +
            "(id, schema, name, full, start, inc, cycle).")
    public List<Map<String, Object>> list_sequences(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return ToolExecutor.run("list sequences", () ->
                sequenceSummaries(metadataQueryService.listSequences(datasourceId, schema)));
    }

    // ===== Export =====

    @Tool(description = "Export metadata as DDL, JSON, or MARKDOWN format. " +
            "Use tableName to export a specific table (recommended). " +
            "Output is capped at " + MAX_EXPORT_CHARS + " characters; filter by schema/table to get complete results.")
    public String export_metadata(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Export format: DDL, JSON, or MARKDOWN") String format,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "Table name to filter by (optional, recommended for large schemas)", required = false) String tableName) {
        return ToolExecutor.run("export metadata", () -> {
            String result = metadataExportService.exportMetadata(datasourceId, format, schema, tableName);
            if (result.length() > MAX_EXPORT_CHARS) {
                return result.substring(0, MAX_EXPORT_CHARS) +
                        "\n\n...[Output truncated at " + MAX_EXPORT_CHARS + " chars. Use schema and tableName filters to export specific tables.]";
            }
            return result;
        });
    }

    // ===== Summary =====

    @Tool(description = "Get a dashboard-style overview of a datasource: total counts for schemas/tables/columns/routines/sequences, " +
            "table type distribution, column type distribution top N, tables with most columns/indexes, and last crawl timing")
    public DatasourceSummary get_datasource_summary(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("get datasource summary", () ->
                datasourceSummaryService.getSummary(datasourceId));
    }

    // ===== Health =====

    @Tool(description = "Check metadata freshness and consistency: last crawl time, freshness status " +
            "(FRESH/AGING/STALE/NO_DATA), crawled vs live table count comparison, connection reachability, " +
            "overall health (HEALTHY/WARNING/UNHEALTHY), and specific warnings")
    public MetadataHealthResponse check_metadata_health(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("check metadata health", () ->
                metadataHealthService.checkHealth(datasourceId));
    }

    // ===== Advanced Search =====

    @Tool(description = "Advanced multi-criteria search across tables and columns. " +
            "Returns compact summaries, capped at " + MAX_SEARCH_TABLES + " tables and " + MAX_SEARCH_COLUMNS + " columns. " +
            "Table filters: schemaName, tableType, importanceLevel, businessDomain, tableNamePattern. " +
            "Column filters: dataType, sensitivityLevel, nullable, partOfPrimaryKey, partOfForeignKey, columnNamePattern.")
    public Map<String, Object> advanced_search(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name filter (optional)", required = false) String schemaName,
            @ToolParam(description = "Table type filter, e.g. TABLE, VIEW (optional)", required = false) String tableType,
            @ToolParam(description = "Importance level: CORE, IMPORTANT, NORMAL, TRIVIAL (optional)", required = false) String importanceLevel,
            @ToolParam(description = "Business domain filter (optional)", required = false) String businessDomain,
            @ToolParam(description = "Table name pattern (substring match, optional)", required = false) String tableNamePattern,
            @ToolParam(description = "Column data type filter (optional)", required = false) String dataType,
            @ToolParam(description = "Sensitivity level: PUBLIC, INTERNAL, SENSITIVE, HIGHLY_SENSITIVE (optional)", required = false) String sensitivityLevel,
            @ToolParam(description = "Filter by nullable columns (optional)", required = false) Boolean nullable,
            @ToolParam(description = "Filter by primary key columns (optional)", required = false) Boolean partOfPrimaryKey,
            @ToolParam(description = "Filter by foreign key columns (optional)", required = false) Boolean partOfForeignKey,
            @ToolParam(description = "Column name pattern (substring match, optional)", required = false) String columnNamePattern) {
        return ToolExecutor.run("perform advanced search", () -> {
            AdvancedSearchRequest req = new AdvancedSearchRequest();
            req.setSchemaName(schemaName);
            req.setTableType(tableType);
            req.setImportanceLevel(importanceLevel);
            req.setBusinessDomain(businessDomain);
            req.setTableNamePattern(tableNamePattern);
            req.setDataType(dataType);
            req.setSensitivityLevel(sensitivityLevel);
            req.setNullable(nullable);
            req.setPartOfPrimaryKey(partOfPrimaryKey);
            req.setPartOfForeignKey(partOfForeignKey);
            req.setColumnNamePattern(columnNamePattern);
            return compactAdvancedSearch(metadataQueryService.advancedSearch(datasourceId, req));
        });
    }
}
