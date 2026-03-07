package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.*;
import com.rock.metadata.model.*;

import java.util.*;

/**
 * MCP tool response compression: shorter keys, null omission, internal field trimming.
 */
public final class McpResponseHelper {

    private McpResponseHelper() {}

    // --- Truncation lengths ---
    static final int SHORT_TEXT = 100;
    static final int MEDIUM_TEXT = 200;
    static final int LONG_TEXT = 500;
    static final int CELL_VALUE = 200;

    // --- Result limits ---
    static final int MAX_SEARCH_TABLES = 50;
    static final int MAX_SEARCH_COLUMNS = 100;
    static final int MAX_EXPORT_CHARS = 50_000;
    static final int MCP_DEFAULT_SQL_ROWS = 100;
    static final int MCP_MAX_SQL_ROWS = 500;
    static final int MCP_DEFAULT_SAMPLE_ROWS = 5;

    // ========== Helpers ==========

    private static void put(Map<String, Object> m, String key, Object value) {
        if (value != null) m.put(key, value);
    }

    private static void putTrue(Map<String, Object> m, String key, boolean value) {
        if (value) m.put(key, true);
    }

    public static String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) return value;
        return value.substring(0, maxLen) + "...";
    }

    public static Map<String, Object> truncateRow(Map<String, Object> row, int maxCellLen) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof String s && s.length() > maxCellLen) {
                result.put(entry.getKey(), s.substring(0, maxCellLen) + "...");
            } else {
                result.put(entry.getKey(), val);
            }
        }
        return result;
    }

    // ========== MetaSchema ==========

    public static Map<String, Object> compact(MetaSchema s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        put(m, "catalog", s.getCatalogName());
        put(m, "schema", s.getSchemaName());
        m.put("full", s.getFullName());
        put(m, "remarks", s.getRemarks());
        put(m, "display", s.getDisplayName());
        put(m, "bizDesc", s.getBusinessDescription());
        put(m, "owner", s.getOwner());
        return m;
    }

    // ========== MetaTable ==========

    public static Map<String, Object> tableSummary(MetaTable t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        put(m, "schema", t.getSchemaName());
        m.put("name", t.getTableName());
        m.put("full", t.getFullName());
        put(m, "type", t.getTableType());
        put(m, "rowCount", t.getRowCount());
        put(m, "comment", truncate(t.getRemarks(), SHORT_TEXT));
        put(m, "display", t.getDisplayName());
        put(m, "domain", t.getBusinessDomain());
        put(m, "importance", t.getImportanceLevel());
        return m;
    }

    public static List<Map<String, Object>> tableSummaries(List<MetaTable> tables) {
        return tables.stream().map(McpResponseHelper::tableSummary).toList();
    }

    public static Map<String, Object> compact(MetaTable t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        put(m, "schema", t.getSchemaName());
        m.put("name", t.getTableName());
        m.put("full", t.getFullName());
        put(m, "type", t.getTableType());
        put(m, "remarks", truncate(t.getRemarks(), LONG_TEXT));
        put(m, "def", truncate(t.getDefinition(), LONG_TEXT));
        put(m, "rowCount", t.getRowCount());
        put(m, "display", t.getDisplayName());
        put(m, "bizDesc", truncate(t.getBusinessDescription(), LONG_TEXT));
        put(m, "domain", t.getBusinessDomain());
        put(m, "owner", t.getOwner());
        put(m, "importance", t.getImportanceLevel());
        put(m, "qualityScore", t.getDataQualityScore());
        put(m, "confidence", t.getAnalysisConfidence());
        return m;
    }

    // ========== MetaColumn ==========

    public static Map<String, Object> columnSummary(MetaColumn c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("tblId", c.getTableId());
        m.put("name", c.getColumnName());
        m.put("pos", c.getOrdinalPosition());
        put(m, "type", c.getDataType());
        m.put("size", c.getColumnSize());
        m.put("nullable", c.isNullable());
        putTrue(m, "pk", c.isPartOfPrimaryKey());
        putTrue(m, "fk", c.isPartOfForeignKey());
        put(m, "comment", truncate(c.getRemarks(), SHORT_TEXT));
        put(m, "display", c.getDisplayName());
        put(m, "sensitivity", c.getSensitivityLevel());
        return m;
    }

    public static List<Map<String, Object>> columnSummaries(List<MetaColumn> columns) {
        return columns.stream().map(McpResponseHelper::columnSummary).toList();
    }

    public static Map<String, Object> compact(MetaColumn c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("tblId", c.getTableId());
        m.put("name", c.getColumnName());
        m.put("pos", c.getOrdinalPosition());
        put(m, "type", c.getDataType());
        put(m, "dbType", c.getDbSpecificTypeName());
        m.put("size", c.getColumnSize());
        if (c.getDecimalDigits() > 0) m.put("decimals", c.getDecimalDigits());
        m.put("nullable", c.isNullable());
        put(m, "default", c.getDefaultValue());
        putTrue(m, "autoInc", c.isAutoIncremented());
        putTrue(m, "generated", c.isGenerated());
        putTrue(m, "hidden", c.isHidden());
        put(m, "colDef", c.getColumnDefinition());
        putTrue(m, "pk", c.isPartOfPrimaryKey());
        putTrue(m, "fk", c.isPartOfForeignKey());
        putTrue(m, "indexed", c.isPartOfIndex());
        put(m, "remarks", truncate(c.getRemarks(), LONG_TEXT));
        put(m, "display", c.getDisplayName());
        put(m, "bizDesc", truncate(c.getBusinessDescription(), LONG_TEXT));
        put(m, "bizType", c.getBusinessDataType());
        put(m, "samples", c.getSampleValues());
        put(m, "range", c.getValueRange());
        put(m, "sensitivity", c.getSensitivityLevel());
        put(m, "sensitivityType", c.getSensitivityType());
        put(m, "masking", c.getMaskingStrategy());
        put(m, "compliance", c.getComplianceFlags());
        put(m, "confidence", c.getAnalysisConfidence());
        return m;
    }

    // ========== MetaPrimaryKey ==========

    public static Map<String, Object> compact(MetaPrimaryKey pk) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pk.getId());
        put(m, "constraint", pk.getConstraintName());
        m.put("col", pk.getColumnName());
        m.put("seq", pk.getKeySequence());
        return m;
    }

    // ========== MetaForeignKey ==========

    public static Map<String, Object> compact(MetaForeignKey fk) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", fk.getId());
        put(m, "name", fk.getFkName());
        m.put("fkCol", fk.getFkColumnName());
        m.put("pkTable", fk.getPkTableFullName());
        m.put("pkCol", fk.getPkColumnName());
        put(m, "onUpdate", fk.getUpdateRule());
        put(m, "onDelete", fk.getDeleteRule());
        return m;
    }

    // ========== MetaIndex ==========

    public static Map<String, Object> compact(MetaIndex idx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", idx.getId());
        put(m, "name", idx.getIndexName());
        m.put("col", idx.getColumnName());
        m.put("pos", idx.getOrdinalPosition());
        put(m, "type", idx.getIndexType());
        putTrue(m, "unique", idx.isUnique());
        put(m, "sort", idx.getSortSequence());
        put(m, "def", truncate(idx.getDefinition(), MEDIUM_TEXT));
        put(m, "remarks", truncate(idx.getRemarks(), SHORT_TEXT));
        return m;
    }

    // ========== MetaTrigger ==========

    public static Map<String, Object> compact(MetaTrigger tr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", tr.getId());
        put(m, "name", tr.getTriggerName());
        put(m, "event", tr.getEventManipulationType());
        put(m, "orientation", tr.getActionOrientation());
        put(m, "timing", tr.getConditionTiming());
        put(m, "action", truncate(tr.getActionStatement(), MEDIUM_TEXT));
        return m;
    }

    // ========== MetaConstraint ==========

    public static Map<String, Object> compact(MetaConstraint c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        put(m, "name", c.getConstraintName());
        put(m, "type", c.getConstraintType());
        put(m, "cols", c.getColumnNames());
        put(m, "def", truncate(c.getDefinition(), MEDIUM_TEXT));
        putTrue(m, "deferrable", c.isDeferrable());
        putTrue(m, "deferred", c.isInitiallyDeferred());
        return m;
    }

    // ========== MetaRoutine ==========

    public static Map<String, Object> routineSummary(MetaRoutine r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        put(m, "schema", r.getSchemaName());
        m.put("name", r.getRoutineName());
        m.put("full", r.getFullName());
        put(m, "type", r.getRoutineType());
        put(m, "returnType", r.getReturnType());
        put(m, "remarks", truncate(r.getRemarks(), SHORT_TEXT));
        return m;
    }

    public static List<Map<String, Object>> routineSummaries(List<MetaRoutine> routines) {
        return routines.stream().map(McpResponseHelper::routineSummary).toList();
    }

    public static Map<String, Object> compact(MetaRoutine r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        put(m, "schema", r.getSchemaName());
        m.put("name", r.getRoutineName());
        m.put("full", r.getFullName());
        put(m, "type", r.getRoutineType());
        put(m, "returnType", r.getReturnType());
        put(m, "def", r.getDefinition());
        put(m, "remarks", r.getRemarks());
        return m;
    }

    // ========== MetaRoutineColumn ==========

    public static Map<String, Object> compact(MetaRoutineColumn c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getColumnName());
        m.put("pos", c.getOrdinalPosition());
        put(m, "mode", c.getColumnType());
        put(m, "type", c.getDataType());
        if (c.getPrecision() > 0) m.put("precision", c.getPrecision());
        if (c.getScale() > 0) m.put("scale", c.getScale());
        m.put("nullable", c.isNullable());
        return m;
    }

    // ========== MetaSequence ==========

    public static Map<String, Object> sequenceSummary(MetaSequence s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        put(m, "schema", s.getSchemaName());
        m.put("name", s.getSequenceName());
        m.put("full", s.getFullName());
        put(m, "start", s.getStartValue());
        put(m, "inc", s.getIncrement());
        putTrue(m, "cycle", s.isCycle());
        return m;
    }

    public static List<Map<String, Object>> sequenceSummaries(List<MetaSequence> sequences) {
        return sequences.stream().map(McpResponseHelper::sequenceSummary).toList();
    }

    // ========== DataSourceConfig ==========

    public static Map<String, Object> compact(DataSourceConfig ds) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ds.getId());
        m.put("name", ds.getName());
        put(m, "desc", ds.getDescription());
        m.put("dbType", ds.getDbType());
        put(m, "host", ds.getHost());
        put(m, "port", ds.getPort());
        put(m, "dbName", ds.getDatabaseName());
        put(m, "user", ds.getUsername());
        put(m, "jdbcUrl", ds.getJdbcUrl());
        put(m, "schemas", ds.getSchemaPatterns());
        return m;
    }

    // ========== CrawlJob ==========

    public static Map<String, Object> compact(CrawlJob job) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", job.getId());
        m.put("dsId", job.getDatasourceId());
        m.put("status", job.getStatus());
        put(m, "level", job.getInfoLevel());
        put(m, "tables", job.getTableCount());
        put(m, "columns", job.getColumnCount());
        put(m, "routines", job.getRoutineCount());
        put(m, "sequences", job.getSequenceCount());
        put(m, "error", truncate(job.getErrorMessage(), MEDIUM_TEXT));
        put(m, "startedAt", job.getStartedAt());
        put(m, "finishedAt", job.getFinishedAt());
        return m;
    }

    // ========== QualityRule ==========

    public static Map<String, Object> compact(QualityRule r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("code", r.getRuleCode());
        m.put("name", r.getRuleName());
        m.put("type", r.getRuleType());
        put(m, "desc", r.getDescription());
        m.put("severity", r.getDefaultSeverity());
        put(m, "params", r.getDefaultParams());
        putTrue(m, "builtIn", r.isBuiltIn());
        if (!r.isActive()) m.put("active", false);
        return m;
    }

    // ========== ColumnQualityRule ==========

    public static Map<String, Object> compact(ColumnQualityRule cr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cr.getId());
        m.put("ruleId", cr.getRuleId());
        m.put("dsId", cr.getDatasourceId());
        put(m, "schema", cr.getSchemaName());
        m.put("table", cr.getTableName());
        m.put("col", cr.getColumnName());
        put(m, "colId", cr.getMetaColumnId());
        put(m, "severity", cr.getSeverity());
        put(m, "params", cr.getParams());
        if (!cr.isEnabled()) m.put("enabled", false);
        return m;
    }

    // ========== DictDefinition ==========

    public static Map<String, Object> compact(DictDefinition d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("code", d.getDictCode());
        m.put("name", d.getDictName());
        m.put("type", d.getDictType());
        put(m, "desc", d.getDescription());
        put(m, "version", d.getVersion());
        if (!d.isActive()) m.put("active", false);
        m.put("srcType", d.getSourceType());
        put(m, "dsId", d.getDatasourceId());
        put(m, "srcSchema", d.getSourceSchemaName());
        put(m, "srcTable", d.getSourceTableName());
        put(m, "srcInfo", d.getSourceInfo());
        return m;
    }

    // ========== DictItem ==========

    public static Map<String, Object> compact(DictItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("dictId", i.getDictId());
        put(m, "parent", i.getParentId());
        m.put("code", i.getItemCode());
        m.put("value", i.getItemValue());
        put(m, "desc", i.getItemDescription());
        put(m, "sort", i.getSortOrder());
        put(m, "level", i.getTreeLevel());
        put(m, "ext", i.getExtAttrs());
        if (!i.isActive()) m.put("active", false);
        return m;
    }

    // ========== DictColumnBinding ==========

    public static Map<String, Object> compact(DictColumnBinding b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("dictId", b.getDictId());
        m.put("dsId", b.getDatasourceId());
        put(m, "schema", b.getSchemaName());
        m.put("table", b.getTableName());
        m.put("col", b.getColumnName());
        put(m, "colId", b.getMetaColumnId());
        m.put("type", b.getBindingType());
        put(m, "confidence", b.getConfidence());
        return m;
    }

    // ========== MetaTag ==========

    public static Map<String, Object> compact(MetaTag t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("type", t.getTargetType());
        m.put("target", t.getTargetId());
        m.put("key", t.getTagKey());
        put(m, "value", t.getTagValue());
        put(m, "source", t.getSource());
        return m;
    }

    // ========== LlmAnalysisJob ==========

    public static Map<String, Object> compact(LlmAnalysisJob j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getId());
        m.put("dsId", j.getDatasourceId());
        m.put("scope", j.getAnalysisScope());
        put(m, "targetId", j.getScopeTargetId());
        put(m, "model", j.getModelName());
        put(m, "type", j.getAnalysisType());
        m.put("status", j.getStatus());
        put(m, "error", truncate(j.getErrorMessage(), MEDIUM_TEXT));
        put(m, "tables", j.getTablesAnalyzed());
        put(m, "columns", j.getColumnsAnalyzed());
        put(m, "tokens", j.getTotalTokens());
        put(m, "startedAt", j.getStartedAt());
        put(m, "finishedAt", j.getFinishedAt());
        return m;
    }

    // ========== DatasetDefinition ==========

    public static Map<String, Object> compact(DatasetDefinition d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("code", d.getDatasetCode());
        m.put("name", d.getDatasetName());
        put(m, "desc", d.getDescription());
        put(m, "domain", d.getBusinessDomain());
        m.put("dsId", d.getDatasourceId());
        m.put("version", d.getVersion());
        m.put("status", d.getStatus());
        put(m, "format", d.getOutputFormat());
        put(m, "rootNode", d.getRootNodeCode());
        put(m, "timeout", d.getMaxExecutionTimeSeconds());
        put(m, "owner", d.getOwner());
        return m;
    }

    // ========== DatasetNode ==========

    public static Map<String, Object> compact(DatasetNode n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("code", n.getNodeCode());
        m.put("name", n.getNodeName());
        put(m, "schema", n.getSourceSchema());
        m.put("table", n.getSourceTable());
        m.put("type", n.getNodeType());
        put(m, "parent", n.getParentNodeCode());
        put(m, "order", n.getExecutionOrder());
        put(m, "cardinality", n.getCardinality());
        put(m, "maxRows", n.getMaxRows());
        if (!n.isEnabled()) m.put("enabled", false);
        return m;
    }

    // ========== DatasetNodeRelation ==========

    public static Map<String, Object> compact(DatasetNodeRelation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("parent", r.getParentNodeCode());
        m.put("child", r.getChildNodeCode());
        put(m, "type", r.getRelationType());
        put(m, "parentCol", r.getParentJoinColumn());
        put(m, "childCol", r.getChildJoinColumn());
        put(m, "expr", r.getJoinExpression());
        put(m, "mode", r.getJoinMode());
        if (!r.isEnabled()) m.put("enabled", false);
        return m;
    }

    // ========== DatasetNodeFilter ==========

    public static Map<String, Object> compact(DatasetNodeFilter f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("node", f.getNodeCode());
        put(m, "name", f.getFilterName());
        m.put("expr", f.getFilterExpression());
        putTrue(m, "parameterized", f.isParameterized());
        put(m, "param", f.getParamName());
        put(m, "pType", f.getParamType());
        put(m, "defaultVal", f.getDefaultValue());
        putTrue(m, "required", f.isRequired());
        if (!f.isEnabled()) m.put("enabled", false);
        return m;
    }

    // ========== DatasetFieldMapping ==========

    public static Map<String, Object> compact(DatasetFieldMapping fm) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", fm.getId());
        m.put("node", fm.getNodeCode());
        m.put("src", fm.getSourceField());
        m.put("out", fm.getOutputField());
        put(m, "type", fm.getOutputType());
        put(m, "ruleId", fm.getTransformRuleId());
        put(m, "expr", fm.getInlineExpression());
        put(m, "defaultVal", fm.getDefaultValue());
        putTrue(m, "required", fm.isRequired());
        if (!fm.isEnabled()) m.put("enabled", false);
        return m;
    }

    // ========== DatasetInstance ==========

    public static Map<String, Object> compact(DatasetInstance i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("datasetId", i.getDatasetId());
        m.put("dsId", i.getDatasourceId());
        m.put("version", i.getDatasetVersion());
        m.put("status", i.getExecutionStatus());
        put(m, "params", i.getExecutionParams());
        put(m, "rootKey", i.getRootKeyValue());
        m.put("total", i.getTotalNodes());
        m.put("success", i.getSuccessNodes());
        m.put("failed", i.getFailedNodes());
        m.put("rows", i.getTotalRows());
        put(m, "error", truncate(i.getErrorMessage(), MEDIUM_TEXT));
        put(m, "progress", i.getNodeProgress());
        put(m, "startedAt", i.getStartedAt());
        put(m, "finishedAt", i.getFinishedAt());
        return m;
    }

    // ========== Composite Response Helpers ==========

    public static Map<String, Object> compactTableDetail(TableDetailResponse detail) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", compact(detail.getTable()));
        result.put("columns", columnSummaries(detail.getColumns()));

        if (detail.getPrimaryKeys() != null && !detail.getPrimaryKeys().isEmpty()) {
            result.put("pks", detail.getPrimaryKeys().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getForeignKeys() != null && !detail.getForeignKeys().isEmpty()) {
            result.put("fks", detail.getForeignKeys().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getIndexes() != null && !detail.getIndexes().isEmpty()) {
            result.put("indexes", detail.getIndexes().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getTriggers() != null && !detail.getTriggers().isEmpty()) {
            result.put("triggers", detail.getTriggers().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getConstraints() != null && !detail.getConstraints().isEmpty()) {
            result.put("constraints", detail.getConstraints().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getPrivileges() != null && !detail.getPrivileges().isEmpty()) {
            result.put("privilegeCount", detail.getPrivileges().size());
        }
        return result;
    }

    public static Map<String, Object> compactRoutineDetail(RoutineDetailResponse detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("routine", compact(detail.getRoutine()));
        if (detail.getColumns() != null && !detail.getColumns().isEmpty()) {
            m.put("params", detail.getColumns().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        return m;
    }

    public static Map<String, Object> compactDictDetail(DictDetailResponse detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("definition", compact(detail.getDefinition()));
        if (detail.getItems() != null) {
            m.put("items", detail.getItems().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        return m;
    }

    public static Map<String, Object> compactDatasetDetail(DatasetDetailResponse detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("definition", compact(detail.getDefinition()));
        if (detail.getNodes() != null) {
            m.put("nodes", detail.getNodes().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getRelations() != null) {
            m.put("relations", detail.getRelations().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getFilters() != null) {
            m.put("filters", detail.getFilters().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        if (detail.getFieldMappings() != null) {
            m.put("fieldMappings", detail.getFieldMappings().stream()
                    .map(McpResponseHelper::compact).toList());
        }
        return m;
    }

    public static Map<String, Object> compactSearchResult(SearchResult result) {
        Map<String, Object> out = new LinkedHashMap<>();

        List<MetaTable> tables = result.getTables();
        boolean tablesTruncated = tables != null && tables.size() > MAX_SEARCH_TABLES;
        List<MetaTable> limitedTables = tablesTruncated ? tables.subList(0, MAX_SEARCH_TABLES) : tables;
        out.put("tables", limitedTables != null ? tableSummaries(limitedTables) : List.of());
        out.put("tableCount", tables != null ? tables.size() : 0);

        List<SearchResult.ColumnMatch> columns = result.getColumns();
        boolean colsTruncated = columns != null && columns.size() > MAX_SEARCH_COLUMNS;
        List<SearchResult.ColumnMatch> limitedCols = colsTruncated ? columns.subList(0, MAX_SEARCH_COLUMNS) : columns;
        if (limitedCols != null) {
            out.put("columns", limitedCols.stream().map(cm -> {
                Map<String, Object> colMap = columnSummary(cm.getColumn());
                colMap.put("tableFull", cm.getTableFullName());
                return colMap;
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

    public static Map<String, Object> compactAdvancedSearch(AdvancedSearchResponse result) {
        Map<String, Object> out = new LinkedHashMap<>();

        List<MetaTable> tables = result.getTables();
        boolean tablesTruncated = tables != null && tables.size() > MAX_SEARCH_TABLES;
        List<MetaTable> limitedTables = tablesTruncated ? tables.subList(0, MAX_SEARCH_TABLES) : tables;
        out.put("tables", limitedTables != null ? tableSummaries(limitedTables) : List.of());
        out.put("tableCount", result.getTableCount());

        List<AdvancedSearchResponse.ColumnResult> columns = result.getColumns();
        boolean colsTruncated = columns != null && columns.size() > MAX_SEARCH_COLUMNS;
        List<AdvancedSearchResponse.ColumnResult> limitedCols = colsTruncated ? columns.subList(0, MAX_SEARCH_COLUMNS) : columns;
        if (limitedCols != null) {
            out.put("columns", limitedCols.stream().map(cr -> {
                Map<String, Object> colMap = columnSummary(cr.getColumn());
                colMap.put("tableFull", cr.getTableFullName());
                return colMap;
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
