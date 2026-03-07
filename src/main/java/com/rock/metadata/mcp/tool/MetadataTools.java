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

import java.util.LinkedHashMap;
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

    @Tool(description = "List all schemas from the latest successful crawl of a datasource")
    public List<MetaSchema> list_schemas(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("list schemas", () ->
                metadataQueryService.listSchemas(datasourceId));
    }

    @Tool(description = "List tables from the latest successful crawl. Returns summary fields only " +
            "(id, schemaName, tableName, fullName, tableType, tableComment, displayName, businessDomain, importanceLevel). " +
            "Use get_table_detail for full information on a specific table.")
    public List<Map<String, Object>> list_tables(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "If true, only return tables not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly) {
        return ToolExecutor.run("list tables", () ->
                toTableSummaries(metadataQueryService.listTables(datasourceId, schema, unanalyzedOnly)));
    }

    @Tool(description = "Get full table detail including columns, primary keys, foreign keys, " +
            "indexes, triggers, constraints, and privileges. Large text fields are truncated; " +
            "use export_metadata for complete DDL.")
    public Map<String, Object> get_table_detail(
            @ToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("get table detail", () -> {
            TableDetailResponse detail = metadataQueryService.getTableDetail(tableId);
            return truncateTableDetail(detail);
        });
    }

    @Tool(description = "List all columns of a table ordered by ordinal position. Returns summary fields only " +
            "(id, tableId, columnName, ordinalPosition, dataType, columnSize, nullable, partOfPrimaryKey, " +
            "partOfForeignKey, columnComment, displayName, sensitivityLevel). " +
            "Use get_table_detail for full column information.")
    public List<Map<String, Object>> list_columns(
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "If true, only return columns not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly) {
        return ToolExecutor.run("list columns", () ->
                toColumnSummaries(metadataQueryService.listColumns(tableId, unanalyzedOnly)));
    }

    @Tool(description = "List foreign keys of a table")
    public List<MetaForeignKey> list_foreign_keys(
            @ToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("list foreign keys", () ->
                metadataQueryService.listForeignKeys(tableId));
    }

    @Tool(description = "List indexes of a table")
    public List<MetaIndex> list_indexes(
            @ToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("list indexes", () ->
                metadataQueryService.listIndexes(tableId));
    }

    @Tool(description = "Get actual row counts for tables by connecting to the target datasource. " +
            "Optionally filter by schema name or table name. Returns row count for each matching table.")
    public List<TableRowCount> count_table_rows(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "Table name to filter by (optional)", required = false) String tableName) {
        return ToolExecutor.run("count table rows", () -> {
            List<MetaTable> tables = metadataQueryService.listTables(datasourceId, schema, null);
            if (tableName != null && !tableName.isBlank()) {
                tables = tables.stream()
                        .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                        .toList();
            }
            return sqlExecuteService.countTableRows(datasourceId, tables);
        });
    }

    @Tool(description = "Search tables and columns by keyword across a datasource's latest crawl. " +
            "Returns summary fields only, capped at " + MAX_SEARCH_TABLES + " tables and " + MAX_SEARCH_COLUMNS + " columns.")
    public Map<String, Object> search_metadata(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Search keyword") String keyword) {
        return ToolExecutor.run("search metadata", () -> {
            SearchResult result = metadataQueryService.search(datasourceId, keyword);
            return truncateSearchResult(result);
        });
    }

    // ===== Routines =====

    @Tool(description = "List stored procedures and functions. Returns summary fields only " +
            "(id, schemaName, routineName, fullName, routineType, returnType, remarks). " +
            "Use get_routine_detail for full definition source code.")
    public List<Map<String, Object>> list_routines(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return ToolExecutor.run("list routines", () ->
                toRoutineSummaries(metadataQueryService.listRoutines(datasourceId, schema)));
    }

    @Tool(description = "Get routine detail including parameter list and full definition source code")
    public RoutineDetailResponse get_routine_detail(
            @ToolParam(description = "Routine ID") Long routineId) {
        return ToolExecutor.run("get routine detail", () ->
                metadataQueryService.getRoutineDetail(routineId));
    }

    // ===== Sequences =====

    @Tool(description = "List sequences from the latest successful crawl. Returns summary fields only.")
    public List<Map<String, Object>> list_sequences(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return ToolExecutor.run("list sequences", () ->
                toSequenceSummaries(metadataQueryService.listSequences(datasourceId, schema)));
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
            "Returns summary fields only, capped at " + MAX_SEARCH_TABLES + " tables and " + MAX_SEARCH_COLUMNS + " columns. " +
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
            AdvancedSearchResponse result = metadataQueryService.advancedSearch(datasourceId, req);
            return truncateAdvancedSearchResult(result);
        });
    }

    // ========== Private helpers ==========

    private Map<String, Object> truncateTableDetail(TableDetailResponse detail) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Table: keep full but truncate large text fields
        MetaTable t = detail.getTable();
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("id", t.getId());
        table.put("schemaName", t.getSchemaName());
        table.put("tableName", t.getTableName());
        table.put("fullName", t.getFullName());
        table.put("tableType", t.getTableType());
        table.put("remarks", truncate(t.getRemarks(), LONG_TEXT));
        table.put("definition", truncate(t.getDefinition(), LONG_TEXT));
        table.put("rowCount", t.getRowCount());
        table.put("displayName", t.getDisplayName());
        table.put("businessDescription", truncate(t.getBusinessDescription(), LONG_TEXT));
        table.put("businessDomain", t.getBusinessDomain());
        table.put("owner", t.getOwner());
        table.put("importanceLevel", t.getImportanceLevel());
        result.put("table", table);

        // Columns: summary form
        result.put("columns", toColumnSummaries(detail.getColumns()));
        result.put("primaryKeys", detail.getPrimaryKeys());
        result.put("foreignKeys", detail.getForeignKeys());
        result.put("indexes", detail.getIndexes());

        // Triggers: truncate action statements
        if (detail.getTriggers() != null) {
            result.put("triggers", detail.getTriggers().stream().map(tr -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", tr.getId());
                m.put("triggerName", tr.getTriggerName());
                m.put("eventManipulationType", tr.getEventManipulationType());
                m.put("actionOrientation", tr.getActionOrientation());
                m.put("conditionTiming", tr.getConditionTiming());
                m.put("actionStatement", truncate(tr.getActionStatement(), MEDIUM_TEXT));
                return m;
            }).toList());
        }

        // Constraints: truncate definitions
        if (detail.getConstraints() != null) {
            result.put("constraints", detail.getConstraints().stream().map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", c.getId());
                m.put("constraintName", c.getConstraintName());
                m.put("constraintType", c.getConstraintType());
                m.put("definition", truncate(c.getDefinition(), MEDIUM_TEXT));
                return m;
            }).toList());
        }

        // Omit privileges (rarely needed, can be large)
        if (detail.getPrivileges() != null && !detail.getPrivileges().isEmpty()) {
            result.put("privilegeCount", detail.getPrivileges().size());
        }

        return result;
    }

    private Map<String, Object> truncateSearchResult(SearchResult result) {
        Map<String, Object> out = new LinkedHashMap<>();

        List<MetaTable> tables = result.getTables();
        boolean tablesTruncated = tables != null && tables.size() > MAX_SEARCH_TABLES;
        List<MetaTable> limitedTables = tablesTruncated ? tables.subList(0, MAX_SEARCH_TABLES) : tables;
        out.put("tables", limitedTables != null ? toTableSummaries(limitedTables) : List.of());
        out.put("tableCount", tables != null ? tables.size() : 0);

        List<SearchResult.ColumnMatch> columns = result.getColumns();
        boolean colsTruncated = columns != null && columns.size() > MAX_SEARCH_COLUMNS;
        List<SearchResult.ColumnMatch> limitedCols = colsTruncated ? columns.subList(0, MAX_SEARCH_COLUMNS) : columns;
        if (limitedCols != null) {
            out.put("columns", limitedCols.stream().map(cm -> {
                Map<String, Object> m = toColumnSummary(cm.getColumn());
                m.put("tableFullName", cm.getTableFullName());
                return m;
            }).toList());
        } else {
            out.put("columns", List.of());
        }
        out.put("columnCount", columns != null ? columns.size() : 0);

        if (tablesTruncated || colsTruncated) {
            out.put("truncated", true);
        }

        return out;
    }

    private Map<String, Object> truncateAdvancedSearchResult(AdvancedSearchResponse result) {
        Map<String, Object> out = new LinkedHashMap<>();

        List<MetaTable> tables = result.getTables();
        boolean tablesTruncated = tables != null && tables.size() > MAX_SEARCH_TABLES;
        List<MetaTable> limitedTables = tablesTruncated ? tables.subList(0, MAX_SEARCH_TABLES) : tables;
        out.put("tables", limitedTables != null ? toTableSummaries(limitedTables) : List.of());
        out.put("tableCount", result.getTableCount());

        List<AdvancedSearchResponse.ColumnResult> columns = result.getColumns();
        boolean colsTruncated = columns != null && columns.size() > MAX_SEARCH_COLUMNS;
        List<AdvancedSearchResponse.ColumnResult> limitedCols = colsTruncated ? columns.subList(0, MAX_SEARCH_COLUMNS) : columns;
        if (limitedCols != null) {
            out.put("columns", limitedCols.stream().map(cr -> {
                Map<String, Object> m = toColumnSummary(cr.getColumn());
                m.put("tableFullName", cr.getTableFullName());
                return m;
            }).toList());
        } else {
            out.put("columns", List.of());
        }
        out.put("columnCount", result.getColumnCount());

        if (tablesTruncated || colsTruncated) {
            out.put("truncated", true);
        }

        return out;
    }
}
