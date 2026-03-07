package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.*;
import com.rock.metadata.model.*;
import com.rock.metadata.service.MetadataExportService;
import com.rock.metadata.service.MetadataHealthService;
import com.rock.metadata.service.MetadataQueryService;
import com.rock.metadata.service.SqlExecuteService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
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
    private final MetadataHealthService metadataHealthService;

    @McpTool(description = "List all schemas from the latest successful crawl of a datasource. " +
            "Returns compact keys: id, catalog, schema, full, remarks, display, bizDesc, owner.")
    public List<Map<String, Object>> list_schemas(
            @McpToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("list schemas", () ->
                metadataQueryService.listSchemas(datasourceId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "List tables from the latest successful crawl. Returns compact summaries " +
            "(id, schema, name, full, type, rowCount, comment, display, domain, importance). " +
            "Returns at most `limit` tables (default 50). Row counts are auto-populated after crawl. " +
            "Use sortByRowCount=true to order by data volume descending. " +
            "For lightweight table name listing, use list_table_names instead.")
    public List<Map<String, Object>> list_tables(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @McpToolParam(description = "If true, only return tables not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly,
            @McpToolParam(description = "If true, sort tables by row count descending (optional)", required = false) Boolean sortByRowCount,
            @McpToolParam(description = "Max number of tables to return (default 50, -1 for all)", required = false) Integer limit) {
        return ToolExecutor.run("list tables", () -> {
            List<MetaTable> tables = metadataQueryService.listTables(datasourceId, schema, unanalyzedOnly,
                    Boolean.TRUE.equals(sortByRowCount));
            int effectiveLimit = (limit == null) ? 50 : limit;
            if (effectiveLimit >= 0 && tables.size() > effectiveLimit) {
                tables = tables.subList(0, effectiveLimit);
            }
            return tableSummaries(tables);
        });
    }

    @McpTool(description = "Lightweight listing of table names. Returns comma-separated full table names (schema.table). " +
            "Supports pagination via offset/limit (default: offset=0, limit=500). " +
            "Response format: 'total:<N>,offset:<N>,count:<N>|name1,name2,...' " +
            "Use namePattern for substring filtering. Ideal for getting a quick overview of all tables in a large database.")
    public String list_table_names(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @McpToolParam(description = "Table name substring filter (optional)", required = false) String namePattern,
            @McpToolParam(description = "If true, sort tables by row count descending (optional)", required = false) Boolean sortByRowCount,
            @McpToolParam(description = "Number of tables to skip (default 0)", required = false) Integer offset,
            @McpToolParam(description = "Max number of table names to return (default 500)", required = false) Integer limit) {
        return ToolExecutor.run("list table names", () -> {
            List<MetaTable> tables = metadataQueryService.listTables(datasourceId, schema, null,
                    Boolean.TRUE.equals(sortByRowCount));
            if (namePattern != null && !namePattern.isBlank()) {
                String pattern = namePattern.toLowerCase();
                tables = tables.stream()
                        .filter(t -> t.getTableName().toLowerCase().contains(pattern))
                        .toList();
            }
            int total = tables.size();
            int effectiveOffset = (offset == null || offset < 0) ? 0 : offset;
            int effectiveLimit = (limit == null) ? 500 : limit;
            if (effectiveOffset >= total) {
                return "total:" + total + ",offset:" + effectiveOffset + ",count:0|";
            }
            int end = Math.min(effectiveOffset + effectiveLimit, total);
            List<MetaTable> page = tables.subList(effectiveOffset, end);
            String names = page.stream().map(MetaTable::getFullName)
                    .collect(java.util.stream.Collectors.joining(","));
            return "total:" + total + ",offset:" + effectiveOffset + ",count:" + page.size() + "|" + names;
        });
    }

    @McpTool(description = "Get full table detail with compact keys. Includes table info, columns (summary), " +
            "pks, fks, indexes, triggers, constraints. Use export_metadata for complete DDL.")
    public Map<String, Object> get_table_detail(
            @McpToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("get table detail", () ->
                compactTableDetail(metadataQueryService.getTableDetail(tableId)));
    }

    @McpTool(description = "List all columns of a table ordered by ordinal position. Returns compact summaries " +
            "(id, tblId, name, pos, type, size, nullable, pk, fk, comment, display, sensitivity). " +
            "Use get_table_detail for full column information.")
    public List<Map<String, Object>> list_columns(
            @McpToolParam(description = "Table ID") Long tableId,
            @McpToolParam(description = "If true, only return columns not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly) {
        return ToolExecutor.run("list columns", () ->
                columnSummaries(metadataQueryService.listColumns(tableId, unanalyzedOnly)));
    }

    @McpTool(description = "List foreign keys of a table with compact keys: id, name, fkCol, pkTable, pkCol, onUpdate, onDelete")
    public List<Map<String, Object>> list_foreign_keys(
            @McpToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("list foreign keys", () ->
                metadataQueryService.listForeignKeys(tableId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "List indexes of a table with compact keys: id, name, col, pos, type, unique, sort")
    public List<Map<String, Object>> list_indexes(
            @McpToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("list indexes", () ->
                metadataQueryService.listIndexes(tableId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Get row counts for tables. By default returns pre-stored counts from metadata " +
            "(auto-populated after crawl). Set refresh=true to re-count by querying the target datasource live. " +
            "Optionally filter by schema name or table name. Results are sorted by row count descending.")
    public List<TableRowCount> count_table_rows(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @McpToolParam(description = "Table name to filter by (optional)", required = false) String tableName,
            @McpToolParam(description = "If true, re-count by querying the target database live instead of using stored counts (optional, default false)", required = false) Boolean refresh) {
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

    @McpTool(description = "Search tables and columns by keyword across a datasource's latest crawl. " +
            "Returns compact summaries, capped at " + MAX_SEARCH_TABLES + " tables and " + MAX_SEARCH_COLUMNS + " columns.")
    public Map<String, Object> search_metadata(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Search keyword") String keyword) {
        return ToolExecutor.run("search metadata", () ->
                compactSearchResult(metadataQueryService.search(datasourceId, keyword)));
    }

    // ===== Routines =====

    @McpTool(description = "List stored procedures and functions. Returns compact summaries " +
            "(id, schema, name, full, type, returnType, remarks). " +
            "Use get_routine_detail for full definition source code.")
    public List<Map<String, Object>> list_routines(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return ToolExecutor.run("list routines", () ->
                routineSummaries(metadataQueryService.listRoutines(datasourceId, schema)));
    }

    @McpTool(description = "Get routine detail including parameter list and full definition source code")
    public Map<String, Object> get_routine_detail(
            @McpToolParam(description = "Routine ID") Long routineId) {
        return ToolExecutor.run("get routine detail", () ->
                compactRoutineDetail(metadataQueryService.getRoutineDetail(routineId)));
    }

    // ===== Sequences =====

    @McpTool(description = "List sequences from the latest successful crawl. Returns compact summaries " +
            "(id, schema, name, full, start, inc, cycle).")
    public List<Map<String, Object>> list_sequences(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return ToolExecutor.run("list sequences", () ->
                sequenceSummaries(metadataQueryService.listSequences(datasourceId, schema)));
    }

    // ===== Export =====

    @McpTool(description = "Export metadata as DDL, JSON, or MARKDOWN format. " +
            "Use tableName to export a specific table (recommended). " +
            "Output is capped at " + MAX_EXPORT_CHARS + " characters; filter by schema/table to get complete results.")
    public String export_metadata(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Export format: DDL, JSON, or MARKDOWN") String format,
            @McpToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @McpToolParam(description = "Table name to filter by (optional, recommended for large schemas)", required = false) String tableName) {
        return ToolExecutor.run("export metadata", () -> {
            String result = metadataExportService.exportMetadata(datasourceId, format, schema, tableName);
            if (result.length() > MAX_EXPORT_CHARS) {
                return result.substring(0, MAX_EXPORT_CHARS) +
                        "\n\n...[Output truncated at " + MAX_EXPORT_CHARS + " chars. Use schema and tableName filters to export specific tables.]";
            }
            return result;
        });
    }

    // ===== Health =====

    @McpTool(description = "Check metadata freshness and consistency: last crawl time, freshness status " +
            "(FRESH/AGING/STALE/NO_DATA), crawled vs live table count comparison, connection reachability, " +
            "overall health (HEALTHY/WARNING/UNHEALTHY), and specific warnings")
    public MetadataHealthResponse check_metadata_health(
            @McpToolParam(description = "Datasource ID") Long datasourceId) {
        return ToolExecutor.run("check metadata health", () ->
                metadataHealthService.checkHealth(datasourceId));
    }

    // ===== Advanced Search =====

    @McpTool(description = "Advanced multi-criteria search across tables and columns. " +
            "Returns compact summaries, capped at " + MAX_SEARCH_TABLES + " tables and " + MAX_SEARCH_COLUMNS + " columns. " +
            "Table filters: schemaName, tableType, importanceLevel, businessDomain, tableNamePattern. " +
            "Column filters: dataType, sensitivityLevel, nullable, partOfPrimaryKey, partOfForeignKey, columnNamePattern.")
    public Map<String, Object> advanced_search(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name filter (optional)", required = false) String schemaName,
            @McpToolParam(description = "Table type filter, e.g. TABLE, VIEW (optional)", required = false) String tableType,
            @McpToolParam(description = "Importance level: CORE, IMPORTANT, NORMAL, TRIVIAL (optional)", required = false) String importanceLevel,
            @McpToolParam(description = "Business domain filter (optional)", required = false) String businessDomain,
            @McpToolParam(description = "Table name pattern (substring match, optional)", required = false) String tableNamePattern,
            @McpToolParam(description = "Column data type filter (optional)", required = false) String dataType,
            @McpToolParam(description = "Sensitivity level: PUBLIC, INTERNAL, SENSITIVE, HIGHLY_SENSITIVE (optional)", required = false) String sensitivityLevel,
            @McpToolParam(description = "Filter by nullable columns (optional)", required = false) Boolean nullable,
            @McpToolParam(description = "Filter by primary key columns (optional)", required = false) Boolean partOfPrimaryKey,
            @McpToolParam(description = "Filter by foreign key columns (optional)", required = false) Boolean partOfForeignKey,
            @McpToolParam(description = "Column name pattern (substring match, optional)", required = false) String columnNamePattern) {
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
