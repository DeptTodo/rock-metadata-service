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
        return metadataQueryService.listSchemas(datasourceId);
    }

    @Tool(description = "List tables from the latest successful crawl, optionally filtered by schema name or unanalyzed status")
    public List<MetaTable> list_tables(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "If true, only return tables not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly) {
        return metadataQueryService.listTables(datasourceId, schema, unanalyzedOnly);
    }

    @Tool(description = "Get full table detail including columns, primary keys, foreign keys, " +
            "indexes, triggers, constraints, and privileges")
    public TableDetailResponse get_table_detail(
            @ToolParam(description = "Table ID") Long tableId) {
        return metadataQueryService.getTableDetail(tableId);
    }

    @Tool(description = "List all columns of a table ordered by ordinal position, optionally filtered by unanalyzed status")
    public List<MetaColumn> list_columns(
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "If true, only return columns not yet analyzed by LLM (optional)", required = false) Boolean unanalyzedOnly) {
        return metadataQueryService.listColumns(tableId, unanalyzedOnly);
    }

    @Tool(description = "List foreign keys of a table")
    public List<MetaForeignKey> list_foreign_keys(
            @ToolParam(description = "Table ID") Long tableId) {
        return metadataQueryService.listForeignKeys(tableId);
    }

    @Tool(description = "List indexes of a table")
    public List<MetaIndex> list_indexes(
            @ToolParam(description = "Table ID") Long tableId) {
        return metadataQueryService.listIndexes(tableId);
    }

    @Tool(description = "Get actual row counts for tables by connecting to the target datasource. " +
            "Optionally filter by schema name or table name. Returns row count for each matching table.")
    public List<TableRowCount> count_table_rows(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "Table name to filter by (optional)", required = false) String tableName) {
        List<MetaTable> tables = metadataQueryService.listTables(datasourceId, schema, null);
        if (tableName != null && !tableName.isBlank()) {
            tables = tables.stream()
                    .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                    .toList();
        }
        return sqlExecuteService.countTableRows(datasourceId, tables);
    }

    @Tool(description = "Search tables and columns by keyword across a datasource's latest crawl")
    public SearchResult search_metadata(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Search keyword") String keyword) {
        return metadataQueryService.search(datasourceId, keyword);
    }

    // ===== Routines =====

    @Tool(description = "List stored procedures and functions from the latest successful crawl, optionally filtered by schema")
    public List<MetaRoutine> list_routines(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return metadataQueryService.listRoutines(datasourceId, schema);
    }

    @Tool(description = "Get routine detail including parameter list")
    public RoutineDetailResponse get_routine_detail(
            @ToolParam(description = "Routine ID") Long routineId) {
        return metadataQueryService.getRoutineDetail(routineId);
    }

    // ===== Sequences =====

    @Tool(description = "List sequences from the latest successful crawl, optionally filtered by schema")
    public List<MetaSequence> list_sequences(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return metadataQueryService.listSequences(datasourceId, schema);
    }

    // ===== Export =====

    @Tool(description = "Export metadata as DDL, JSON, or MARKDOWN format. " +
            "DDL generates CREATE TABLE statements with PK/FK/INDEX. " +
            "JSON provides hierarchical metadata tree. " +
            "MARKDOWN generates readable documentation with column tables.")
    public String export_metadata(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Export format: DDL, JSON, or MARKDOWN") String format,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return metadataExportService.exportMetadata(datasourceId, format, schema);
    }

    // ===== Summary =====

    @Tool(description = "Get a dashboard-style overview of a datasource: total counts for schemas/tables/columns/routines/sequences, " +
            "table type distribution, column type distribution top N, tables with most columns/indexes, and last crawl timing")
    public DatasourceSummary get_datasource_summary(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return datasourceSummaryService.getSummary(datasourceId);
    }

    // ===== Health =====

    @Tool(description = "Check metadata freshness and consistency: last crawl time, freshness status " +
            "(FRESH/AGING/STALE/NO_DATA), crawled vs live table count comparison, connection reachability, " +
            "overall health (HEALTHY/WARNING/UNHEALTHY), and specific warnings")
    public MetadataHealthResponse check_metadata_health(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return metadataHealthService.checkHealth(datasourceId);
    }

    // ===== Advanced Search =====

    @Tool(description = "Advanced multi-criteria search across tables and columns. " +
            "Table filters: schemaName, tableType, importanceLevel, businessDomain, tableNamePattern. " +
            "Column filters: dataType, sensitivityLevel, nullable, partOfPrimaryKey, partOfForeignKey, columnNamePattern.")
    public AdvancedSearchResponse advanced_search(
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
        return metadataQueryService.advancedSearch(datasourceId, req);
    }
}
