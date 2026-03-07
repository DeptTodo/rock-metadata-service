package com.rock.metadata.controller;

import com.rock.metadata.dto.*;
import com.rock.metadata.model.*;
import com.rock.metadata.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataQueryService queryService;
    private final SqlExecuteService sqlExecuteService;
    private final MetadataExportService metadataExportService;
    private final DatasourceSummaryService datasourceSummaryService;
    private final MetadataHealthService metadataHealthService;
    private final SchemaDiffService schemaDiffService;
    private final RelationshipService relationshipService;
    private final DataProfilingService dataProfilingService;

    /** List schemas for a datasource (latest successful crawl). */
    @GetMapping("/datasources/{datasourceId}/schemas")
    public List<MetaSchema> listSchemas(@PathVariable Long datasourceId) {
        return queryService.listSchemas(datasourceId);
    }

    /** List tables for a datasource, optionally filter by schema. */
    @GetMapping("/datasources/{datasourceId}/tables")
    public List<MetaTable> listTables(
            @PathVariable Long datasourceId,
            @RequestParam(required = false) String schema) {
        return queryService.listTables(datasourceId, schema);
    }

    /** Get full detail of a table: columns, primary keys, foreign keys, indexes. */
    @GetMapping("/tables/{tableId}")
    public TableDetailResponse getTableDetail(@PathVariable Long tableId) {
        return queryService.getTableDetail(tableId);
    }

    @GetMapping("/tables/{tableId}/columns")
    public List<MetaColumn> listColumns(@PathVariable Long tableId) {
        return queryService.listColumns(tableId);
    }

    @GetMapping("/tables/{tableId}/foreign-keys")
    public List<MetaForeignKey> listForeignKeys(@PathVariable Long tableId) {
        return queryService.listForeignKeys(tableId);
    }

    @GetMapping("/tables/{tableId}/indexes")
    public List<MetaIndex> listIndexes(@PathVariable Long tableId) {
        return queryService.listIndexes(tableId);
    }

    /** Get row counts for tables in a datasource, optionally filtered by schema or table name. */
    @GetMapping("/datasources/{datasourceId}/table-row-counts")
    public List<TableRowCount> getTableRowCounts(
            @PathVariable Long datasourceId,
            @RequestParam(required = false) String schema,
            @RequestParam(required = false) String tableName) {
        List<MetaTable> tables = queryService.listTables(datasourceId, schema);
        if (tableName != null && !tableName.isBlank()) {
            tables = tables.stream()
                    .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                    .toList();
        }
        return sqlExecuteService.countTableRows(datasourceId, tables);
    }

    /** Search tables and columns by keyword across a datasource. */
    @GetMapping("/datasources/{datasourceId}/search")
    public SearchResult search(
            @PathVariable Long datasourceId,
            @RequestParam String keyword) {
        return queryService.search(datasourceId, keyword);
    }

    // ===== Routines =====

    @GetMapping("/datasources/{datasourceId}/routines")
    public List<MetaRoutine> listRoutines(
            @PathVariable Long datasourceId,
            @RequestParam(required = false) String schema) {
        return queryService.listRoutines(datasourceId, schema);
    }

    @GetMapping("/routines/{routineId}")
    public RoutineDetailResponse getRoutineDetail(@PathVariable Long routineId) {
        return queryService.getRoutineDetail(routineId);
    }

    // ===== Sequences =====

    @GetMapping("/datasources/{datasourceId}/sequences")
    public List<MetaSequence> listSequences(
            @PathVariable Long datasourceId,
            @RequestParam(required = false) String schema) {
        return queryService.listSequences(datasourceId, schema);
    }

    // ===== LLM Analysis Jobs =====

    @GetMapping("/llm-analysis-jobs")
    public List<LlmAnalysisJob> listLlmAnalysisJobs(
            @RequestParam(required = false) Long datasourceId) {
        return queryService.listLlmAnalysisJobs(datasourceId);
    }

    @GetMapping("/llm-analysis-jobs/{jobId}")
    public LlmAnalysisJob getLlmAnalysisJob(@PathVariable Long jobId) {
        return queryService.getLlmAnalysisJob(jobId);
    }

    // ===== Export =====

    @GetMapping("/datasources/{datasourceId}/export")
    public String exportMetadata(
            @PathVariable Long datasourceId,
            @RequestParam String format,
            @RequestParam(required = false) String schema) {
        return metadataExportService.exportMetadata(datasourceId, format, schema);
    }

    // ===== Summary =====

    @GetMapping("/datasources/{datasourceId}/summary")
    public DatasourceSummary getDatasourceSummary(@PathVariable Long datasourceId) {
        return datasourceSummaryService.getSummary(datasourceId);
    }

    // ===== Health =====

    @GetMapping("/datasources/{datasourceId}/health")
    public MetadataHealthResponse checkHealth(@PathVariable Long datasourceId) {
        return metadataHealthService.checkHealth(datasourceId);
    }

    // ===== Schema Diff =====

    @GetMapping("/datasources/{datasourceId}/diff")
    public SchemaDiffResponse compareCrawls(
            @PathVariable Long datasourceId,
            @RequestParam(required = false) Long crawlJobId1,
            @RequestParam(required = false) Long crawlJobId2) {
        return schemaDiffService.compareCrawls(datasourceId, crawlJobId1, crawlJobId2);
    }

    // ===== Relationships =====

    @GetMapping("/tables/{tableId}/relationships")
    public TableRelationshipResponse getTableRelationships(
            @PathVariable Long tableId,
            @RequestParam(required = false) Integer depth) {
        return relationshipService.getTableRelationships(tableId, depth);
    }

    @GetMapping("/tables/{tableId}/impact-analysis")
    public ImpactAnalysisResponse getImpactAnalysis(@PathVariable Long tableId) {
        return relationshipService.getImpactAnalysis(tableId);
    }

    // ===== Advanced Search =====

    @PostMapping("/datasources/{datasourceId}/advanced-search")
    public AdvancedSearchResponse advancedSearch(
            @PathVariable Long datasourceId,
            @RequestBody AdvancedSearchRequest request) {
        return queryService.advancedSearch(datasourceId, request);
    }

    // ===== Data Profiling =====

    @GetMapping("/datasources/{datasourceId}/tables/{tableId}/profile")
    public TableProfileResponse profileTable(
            @PathVariable Long datasourceId,
            @PathVariable Long tableId,
            @RequestParam(required = false) List<String> columns) {
        return dataProfilingService.profileTable(datasourceId, tableId, columns);
    }

    @GetMapping("/datasources/{datasourceId}/tables/{tableId}/profile/{columnName}")
    public ColumnProfile profileColumn(
            @PathVariable Long datasourceId,
            @PathVariable Long tableId,
            @PathVariable String columnName) {
        return dataProfilingService.profileSingleColumn(datasourceId, tableId, columnName);
    }
}
