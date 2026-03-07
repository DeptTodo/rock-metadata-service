package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.ColumnQualityCheckResponse;
import com.rock.metadata.service.DataQualityService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataQualityTools {

    private final DataQualityService dataQualityService;

    // ===== QualityRule =====

    @McpTool(description = "Create a data quality rule definition. Rule types: NOT_NULL, UNIQUE, VALUE_RANGE, " +
            "LENGTH_RANGE, REGEX_MATCH, ENUM_VALUES, NOT_BLANK, CUSTOM_SQL. " +
            "Severity levels: CRITICAL, MAJOR, MINOR, INFO. " +
            "Params is a JSON string for rule configuration, e.g. {\"min\":0,\"max\":100} for VALUE_RANGE.")
    public Map<String, Object> create_quality_rule(
            @McpToolParam(description = "Unique rule code, e.g. NOT_NULL, RANGE_0_100") String ruleCode,
            @McpToolParam(description = "Display name for the rule") String ruleName,
            @McpToolParam(description = "Rule type: NOT_NULL, UNIQUE, VALUE_RANGE, LENGTH_RANGE, REGEX_MATCH, ENUM_VALUES, NOT_BLANK, CUSTOM_SQL") String ruleType,
            @McpToolParam(description = "Rule description", required = false) String description,
            @McpToolParam(description = "Default severity: CRITICAL, MAJOR, MINOR, INFO") String defaultSeverity,
            @McpToolParam(description = "Default params as JSON (optional)", required = false) String defaultParams) {
        return ToolExecutor.run("create quality rule", () ->
                McpResponseHelper.compact(dataQualityService.createRule(ruleCode, ruleName, ruleType, description,
                        defaultSeverity, defaultParams)));
    }

    @McpTool(description = "List data quality rule definitions, optionally filtered by rule type or active status")
    public List<Map<String, Object>> list_quality_rules(
            @McpToolParam(description = "Filter by rule type (optional)", required = false) String ruleType,
            @McpToolParam(description = "Only show active rules (optional)", required = false) Boolean activeOnly) {
        return ToolExecutor.run("list quality rules", () ->
                dataQualityService.listRules(ruleType, activeOnly).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Get a data quality rule definition by ID")
    public Map<String, Object> get_quality_rule(
            @McpToolParam(description = "Rule ID") Long ruleId) {
        return ToolExecutor.run("get quality rule", () ->
                McpResponseHelper.compact(dataQualityService.getRule(ruleId)));
    }

    @McpTool(description = "Update a data quality rule definition. Only provided fields will be updated.")
    public Map<String, Object> update_quality_rule(
            @McpToolParam(description = "Rule ID") Long ruleId,
            @McpToolParam(description = "New display name (optional)", required = false) String ruleName,
            @McpToolParam(description = "New description (optional)", required = false) String description,
            @McpToolParam(description = "New default severity (optional)", required = false) String defaultSeverity,
            @McpToolParam(description = "New default params as JSON (optional)", required = false) String defaultParams,
            @McpToolParam(description = "Enable/disable the rule (optional)", required = false) Boolean active) {
        return ToolExecutor.run("update quality rule", () ->
                McpResponseHelper.compact(dataQualityService.updateRule(ruleId, ruleName, description,
                        defaultSeverity, defaultParams, active)));
    }

    @McpTool(description = "Delete a data quality rule definition and all its column bindings. Built-in rules cannot be deleted.")
    public String delete_quality_rule(
            @McpToolParam(description = "Rule ID") Long ruleId) {
        ToolExecutor.runVoid("delete quality rule", () -> dataQualityService.deleteRule(ruleId));
        return "Quality rule " + ruleId + " and its column bindings deleted successfully";
    }

    // ===== ColumnQualityRule =====

    @McpTool(description = "Bind a quality rule to a specific database column. " +
            "Severity and params can override the rule defaults.")
    public Map<String, Object> bind_quality_rule_to_column(
            @McpToolParam(description = "Quality rule ID") Long ruleId,
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @McpToolParam(description = "Table name") String tableName,
            @McpToolParam(description = "Column name") String columnName,
            @McpToolParam(description = "MetaColumn ID (optional)", required = false) Long metaColumnId,
            @McpToolParam(description = "Override severity: CRITICAL, MAJOR, MINOR, INFO (optional)", required = false) String severity,
            @McpToolParam(description = "Override params as JSON (optional)", required = false) String params) {
        return ToolExecutor.run("bind quality rule to column", () ->
                McpResponseHelper.compact(dataQualityService.bindRuleToColumn(ruleId, datasourceId, schemaName,
                        tableName, columnName, metaColumnId, severity, params)));
    }

    @McpTool(description = "List column quality rule bindings, filterable by datasource, table, and column")
    public List<Map<String, Object>> list_column_quality_rules(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @McpToolParam(description = "Table name (optional)", required = false) String tableName,
            @McpToolParam(description = "Column name (optional)", required = false) String columnName) {
        return ToolExecutor.run("list column quality rules", () ->
                dataQualityService.listColumnRules(datasourceId, schemaName, tableName, columnName).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "List column quality rule bindings by MetaColumn ID")
    public List<Map<String, Object>> list_column_quality_rules_by_meta_column(
            @McpToolParam(description = "MetaColumn ID") Long metaColumnId) {
        return ToolExecutor.run("list column quality rules", () ->
                dataQualityService.listColumnRulesByMetaColumn(metaColumnId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Update a column quality rule binding. Only provided fields will be updated.")
    public Map<String, Object> update_column_quality_rule(
            @McpToolParam(description = "Column quality rule binding ID") Long id,
            @McpToolParam(description = "New severity (optional)", required = false) String severity,
            @McpToolParam(description = "New params as JSON (optional)", required = false) String params,
            @McpToolParam(description = "Enable/disable (optional)", required = false) Boolean enabled) {
        return ToolExecutor.run("update column quality rule", () ->
                McpResponseHelper.compact(dataQualityService.updateColumnRule(id, severity, params, enabled)));
    }

    @McpTool(description = "Delete a column quality rule binding")
    public String delete_column_quality_rule(
            @McpToolParam(description = "Column quality rule binding ID") Long id) {
        ToolExecutor.runVoid("delete column quality rule", () -> dataQualityService.deleteColumnRule(id));
        return "Column quality rule binding " + id + " deleted successfully";
    }

    // ===== Quality Check Execution =====

    @McpTool(description = "Execute all enabled quality rules on a specific column against live data. " +
            "Returns violation counts, rates, and sample violating values for each rule.")
    public ColumnQualityCheckResponse execute_column_quality_check(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @McpToolParam(description = "Table name") String tableName,
            @McpToolParam(description = "Column name") String columnName) {
        return ToolExecutor.run("execute column quality check", () ->
                dataQualityService.executeColumnCheck(datasourceId, schemaName, tableName, columnName));
    }

    @McpTool(description = "Execute all enabled quality rules on all columns of a table against live data. " +
            "Returns per-column quality check results.")
    public List<ColumnQualityCheckResponse> execute_table_quality_check(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @McpToolParam(description = "Table name") String tableName) {
        return ToolExecutor.run("execute table quality check", () ->
                dataQualityService.executeTableCheck(datasourceId, schemaName, tableName));
    }
}
