package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.ColumnQualityCheckResponse;
import com.rock.metadata.model.ColumnQualityRule;
import com.rock.metadata.model.QualityRule;
import com.rock.metadata.service.DataQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataQualityTools {

    private final DataQualityService dataQualityService;

    // ===== QualityRule =====

    @Tool(description = "Create a data quality rule definition. Rule types: NOT_NULL, UNIQUE, VALUE_RANGE, " +
            "LENGTH_RANGE, REGEX_MATCH, ENUM_VALUES, NOT_BLANK, CUSTOM_SQL. " +
            "Severity levels: CRITICAL, MAJOR, MINOR, INFO. " +
            "Params is a JSON string for rule configuration, e.g. {\"min\":0,\"max\":100} for VALUE_RANGE.")
    public QualityRule create_quality_rule(
            @ToolParam(description = "Unique rule code, e.g. NOT_NULL, RANGE_0_100") String ruleCode,
            @ToolParam(description = "Display name for the rule") String ruleName,
            @ToolParam(description = "Rule type: NOT_NULL, UNIQUE, VALUE_RANGE, LENGTH_RANGE, REGEX_MATCH, ENUM_VALUES, NOT_BLANK, CUSTOM_SQL") String ruleType,
            @ToolParam(description = "Rule description", required = false) String description,
            @ToolParam(description = "Default severity: CRITICAL, MAJOR, MINOR, INFO") String defaultSeverity,
            @ToolParam(description = "Default params as JSON (optional)", required = false) String defaultParams) {
        return dataQualityService.createRule(ruleCode, ruleName, ruleType, description,
                defaultSeverity, defaultParams);
    }

    @Tool(description = "List data quality rule definitions, optionally filtered by rule type or active status")
    public List<QualityRule> list_quality_rules(
            @ToolParam(description = "Filter by rule type (optional)", required = false) String ruleType,
            @ToolParam(description = "Only show active rules (optional)", required = false) Boolean activeOnly) {
        return dataQualityService.listRules(ruleType, activeOnly);
    }

    @Tool(description = "Get a data quality rule definition by ID")
    public QualityRule get_quality_rule(
            @ToolParam(description = "Rule ID") Long ruleId) {
        return dataQualityService.getRule(ruleId);
    }

    @Tool(description = "Update a data quality rule definition. Only provided fields will be updated.")
    public QualityRule update_quality_rule(
            @ToolParam(description = "Rule ID") Long ruleId,
            @ToolParam(description = "New display name (optional)", required = false) String ruleName,
            @ToolParam(description = "New description (optional)", required = false) String description,
            @ToolParam(description = "New default severity (optional)", required = false) String defaultSeverity,
            @ToolParam(description = "New default params as JSON (optional)", required = false) String defaultParams,
            @ToolParam(description = "Enable/disable the rule (optional)", required = false) Boolean active) {
        return dataQualityService.updateRule(ruleId, ruleName, description, defaultSeverity, defaultParams, active);
    }

    @Tool(description = "Delete a data quality rule definition and all its column bindings. Built-in rules cannot be deleted.")
    public String delete_quality_rule(
            @ToolParam(description = "Rule ID") Long ruleId) {
        dataQualityService.deleteRule(ruleId);
        return "Quality rule " + ruleId + " and its column bindings deleted successfully";
    }

    // ===== ColumnQualityRule =====

    @Tool(description = "Bind a quality rule to a specific database column. " +
            "Severity and params can override the rule defaults.")
    public ColumnQualityRule bind_quality_rule_to_column(
            @ToolParam(description = "Quality rule ID") Long ruleId,
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @ToolParam(description = "Table name") String tableName,
            @ToolParam(description = "Column name") String columnName,
            @ToolParam(description = "MetaColumn ID (optional)", required = false) Long metaColumnId,
            @ToolParam(description = "Override severity: CRITICAL, MAJOR, MINOR, INFO (optional)", required = false) String severity,
            @ToolParam(description = "Override params as JSON (optional)", required = false) String params) {
        return dataQualityService.bindRuleToColumn(ruleId, datasourceId, schemaName, tableName,
                columnName, metaColumnId, severity, params);
    }

    @Tool(description = "List column quality rule bindings, filterable by datasource, table, and column")
    public List<ColumnQualityRule> list_column_quality_rules(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @ToolParam(description = "Table name (optional)", required = false) String tableName,
            @ToolParam(description = "Column name (optional)", required = false) String columnName) {
        return dataQualityService.listColumnRules(datasourceId, schemaName, tableName, columnName);
    }

    @Tool(description = "List column quality rule bindings by MetaColumn ID")
    public List<ColumnQualityRule> list_column_quality_rules_by_meta_column(
            @ToolParam(description = "MetaColumn ID") Long metaColumnId) {
        return dataQualityService.listColumnRulesByMetaColumn(metaColumnId);
    }

    @Tool(description = "Update a column quality rule binding. Only provided fields will be updated.")
    public ColumnQualityRule update_column_quality_rule(
            @ToolParam(description = "Column quality rule binding ID") Long id,
            @ToolParam(description = "New severity (optional)", required = false) String severity,
            @ToolParam(description = "New params as JSON (optional)", required = false) String params,
            @ToolParam(description = "Enable/disable (optional)", required = false) Boolean enabled) {
        return dataQualityService.updateColumnRule(id, severity, params, enabled);
    }

    @Tool(description = "Delete a column quality rule binding")
    public String delete_column_quality_rule(
            @ToolParam(description = "Column quality rule binding ID") Long id) {
        dataQualityService.deleteColumnRule(id);
        return "Column quality rule binding " + id + " deleted successfully";
    }

    // ===== Quality Check Execution =====

    @Tool(description = "Execute all enabled quality rules on a specific column against live data. " +
            "Returns violation counts, rates, and sample violating values for each rule.")
    public ColumnQualityCheckResponse execute_column_quality_check(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @ToolParam(description = "Table name") String tableName,
            @ToolParam(description = "Column name") String columnName) {
        return dataQualityService.executeColumnCheck(datasourceId, schemaName, tableName, columnName);
    }

    @Tool(description = "Execute all enabled quality rules on all columns of a table against live data. " +
            "Returns per-column quality check results.")
    public List<ColumnQualityCheckResponse> execute_table_quality_check(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @ToolParam(description = "Table name") String tableName) {
        return dataQualityService.executeTableCheck(datasourceId, schemaName, tableName);
    }
}
