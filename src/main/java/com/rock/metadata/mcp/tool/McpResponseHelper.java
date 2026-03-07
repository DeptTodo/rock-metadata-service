package com.rock.metadata.mcp.tool;

import com.rock.metadata.model.*;

import java.util.*;

/**
 * Utility for MCP tool response size optimization.
 * Truncates large text fields and converts entities to lightweight summaries.
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

    /**
     * Truncate a string to maxLen, appending "..." if truncated.
     */
    public static String truncate(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) return value;
        return value.substring(0, maxLen) + "...";
    }

    /**
     * Truncate all string values in a row map.
     */
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

    // ========== Table Summary ==========

    public static Map<String, Object> toTableSummary(MetaTable t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("schemaName", t.getSchemaName());
        m.put("tableName", t.getTableName());
        m.put("fullName", t.getFullName());
        m.put("tableType", t.getTableType());
        m.put("tableComment", truncate(t.getRemarks(), SHORT_TEXT));
        m.put("displayName", t.getDisplayName());
        m.put("businessDomain", t.getBusinessDomain());
        m.put("importanceLevel", t.getImportanceLevel());
        return m;
    }

    public static List<Map<String, Object>> toTableSummaries(List<MetaTable> tables) {
        return tables.stream().map(McpResponseHelper::toTableSummary).toList();
    }

    // ========== Column Summary ==========

    public static Map<String, Object> toColumnSummary(MetaColumn c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("tableId", c.getTableId());
        m.put("columnName", c.getColumnName());
        m.put("ordinalPosition", c.getOrdinalPosition());
        m.put("dataType", c.getDataType());
        m.put("columnSize", c.getColumnSize());
        m.put("nullable", c.isNullable());
        m.put("partOfPrimaryKey", c.isPartOfPrimaryKey());
        m.put("partOfForeignKey", c.isPartOfForeignKey());
        m.put("columnComment", truncate(c.getRemarks(), SHORT_TEXT));
        m.put("displayName", c.getDisplayName());
        m.put("sensitivityLevel", c.getSensitivityLevel());
        return m;
    }

    public static List<Map<String, Object>> toColumnSummaries(List<MetaColumn> columns) {
        return columns.stream().map(McpResponseHelper::toColumnSummary).toList();
    }

    // ========== Routine Summary ==========

    public static Map<String, Object> toRoutineSummary(MetaRoutine r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("schemaName", r.getSchemaName());
        m.put("routineName", r.getRoutineName());
        m.put("fullName", r.getFullName());
        m.put("routineType", r.getRoutineType());
        m.put("returnType", r.getReturnType());
        m.put("remarks", truncate(r.getRemarks(), SHORT_TEXT));
        return m;
    }

    public static List<Map<String, Object>> toRoutineSummaries(List<MetaRoutine> routines) {
        return routines.stream().map(McpResponseHelper::toRoutineSummary).toList();
    }

    // ========== Sequence Summary ==========

    public static Map<String, Object> toSequenceSummary(MetaSequence s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("schemaName", s.getSchemaName());
        m.put("sequenceName", s.getSequenceName());
        m.put("fullName", s.getFullName());
        m.put("startValue", s.getStartValue());
        m.put("increment", s.getIncrement());
        m.put("cycle", s.isCycle());
        return m;
    }

    public static List<Map<String, Object>> toSequenceSummaries(List<MetaSequence> sequences) {
        return sequences.stream().map(McpResponseHelper::toSequenceSummary).toList();
    }
}
