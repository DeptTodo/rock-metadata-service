package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.DatasetValidationResponse;
import com.rock.metadata.service.DatasetDefinitionService;
import com.rock.metadata.service.DatasetExecutionService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.rock.metadata.mcp.tool.McpResponseHelper.*;

@Component
@RequiredArgsConstructor
public class DatasetTools {

    private final DatasetDefinitionService definitionService;
    private final DatasetExecutionService executionService;

    // ===== Dataset Definition =====

    @McpTool(description = "Create a new dataset definition. A dataset defines a reusable template " +
            "with aggregation nodes, relations, field mappings and transform rules.")
    public Map<String, Object> create_dataset(
            @McpToolParam(description = "Unique dataset code") String datasetCode,
            @McpToolParam(description = "Display name") String datasetName,
            @McpToolParam(description = "Target datasource ID") Long datasourceId,
            @McpToolParam(description = "Description", required = false) String description,
            @McpToolParam(description = "Business domain", required = false) String businessDomain,
            @McpToolParam(description = "Output format: TREE or FLAT", required = false) String outputFormat,
            @McpToolParam(description = "Root node code", required = false) String rootNodeCode,
            @McpToolParam(description = "Max execution timeout in seconds", required = false) Integer maxExecutionTimeSeconds,
            @McpToolParam(description = "Owner", required = false) String owner) {
        return ToolExecutor.run("create dataset", () ->
                compact(definitionService.createDataset(datasetCode, datasetName, description,
                        businessDomain, datasourceId, outputFormat, rootNodeCode,
                        maxExecutionTimeSeconds, owner)));
    }

    @McpTool(description = "List dataset definitions, optionally filtered by datasourceId, status (DRAFT/PUBLISHED/ARCHIVED), or domain")
    public List<Map<String, Object>> list_datasets(
            @McpToolParam(description = "Datasource ID filter", required = false) Long datasourceId,
            @McpToolParam(description = "Status filter: DRAFT, PUBLISHED, ARCHIVED", required = false) String status,
            @McpToolParam(description = "Business domain filter", required = false) String domain) {
        return ToolExecutor.run("list datasets", () ->
                definitionService.listDatasets(datasourceId, status, domain).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Get complete dataset definition detail including all nodes, relations, filters and field mappings")
    public Map<String, Object> get_dataset_detail(
            @McpToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("get dataset detail", () ->
                compactDatasetDetail(definitionService.getDatasetDetail(datasetId)));
    }

    @McpTool(description = "Update a dataset definition (only allowed in DRAFT status)")
    public Map<String, Object> update_dataset(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "New display name", required = false) String datasetName,
            @McpToolParam(description = "New description", required = false) String description,
            @McpToolParam(description = "Business domain", required = false) String businessDomain,
            @McpToolParam(description = "Output format: TREE or FLAT", required = false) String outputFormat,
            @McpToolParam(description = "Root node code", required = false) String rootNodeCode,
            @McpToolParam(description = "Max execution timeout", required = false) Integer maxExecutionTimeSeconds,
            @McpToolParam(description = "Owner", required = false) String owner) {
        return ToolExecutor.run("update dataset", () ->
                compact(definitionService.updateDataset(datasetId, datasetName, description,
                        businessDomain, outputFormat, rootNodeCode,
                        maxExecutionTimeSeconds, owner)));
    }

    @McpTool(description = "Delete a dataset definition and all its nodes, relations, filters, field mappings and instances")
    public String delete_dataset(
            @McpToolParam(description = "Dataset ID") Long datasetId) {
        ToolExecutor.runVoid("delete dataset", () -> definitionService.deleteDataset(datasetId));
        return "Dataset " + datasetId + " deleted successfully";
    }

    @McpTool(description = "Publish a dataset (DRAFT -> PUBLISHED). Validates the dataset before publishing. " +
            "Only PUBLISHED datasets can be executed.")
    public Map<String, Object> publish_dataset(
            @McpToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("publish dataset", () ->
                compact(definitionService.publishDataset(datasetId)));
    }

    @McpTool(description = "Validate a dataset definition. Checks root node, parent references, " +
            "relation references, field mappings, and detects circular dependencies using Kahn's algorithm.")
    public DatasetValidationResponse validate_dataset(
            @McpToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("validate dataset", () ->
                definitionService.validateDataset(datasetId));
    }

    // ===== Nodes =====

    @McpTool(description = "Add a node to a dataset. Nodes represent source tables in the aggregation tree. " +
            "Node types: ROOT, CHILD_OBJECT, CHILD_LIST, BRIDGE, DERIVED.")
    public Map<String, Object> add_dataset_node(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "Unique node code within the dataset") String nodeCode,
            @McpToolParam(description = "Display name") String nodeName,
            @McpToolParam(description = "Source table name") String sourceTable,
            @McpToolParam(description = "Node type: ROOT, CHILD_OBJECT, CHILD_LIST, BRIDGE, DERIVED") String nodeType,
            @McpToolParam(description = "Source schema name", required = false) String sourceSchema,
            @McpToolParam(description = "Parent node code (null for ROOT)", required = false) String parentNodeCode,
            @McpToolParam(description = "Execution order (lower = earlier)", required = false) Integer executionOrder,
            @McpToolParam(description = "Cardinality: ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY", required = false) String cardinality,
            @McpToolParam(description = "Max rows per node", required = false) Integer maxRows) {
        return ToolExecutor.run("add dataset node", () ->
                compact(definitionService.addNode(datasetId, nodeCode, nodeName, sourceSchema,
                        sourceTable, nodeType, parentNodeCode, executionOrder, cardinality,
                        maxRows, null, null, null)));
    }

    @McpTool(description = "List all nodes of a dataset, ordered by execution order")
    public List<Map<String, Object>> list_dataset_nodes(
            @McpToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("list dataset nodes", () ->
                definitionService.listNodes(datasetId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Delete a node from a dataset (only allowed in DRAFT status)")
    public String delete_dataset_node(
            @McpToolParam(description = "Node ID") Long nodeId) {
        ToolExecutor.runVoid("delete dataset node", () -> definitionService.deleteNode(nodeId));
        return "Node " + nodeId + " deleted successfully";
    }

    // ===== Relations =====

    @McpTool(description = "Add a relation between two nodes. Relation types: FK (foreign key lookup), " +
            "COLUMN_MATCH (explicit column match), CUSTOM_SQL (custom join expression).")
    public Map<String, Object> add_dataset_relation(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "Parent node code") String parentNodeCode,
            @McpToolParam(description = "Child node code") String childNodeCode,
            @McpToolParam(description = "Relation type: FK, COLUMN_MATCH, CUSTOM_SQL", required = false) String relationType,
            @McpToolParam(description = "Parent table join column name", required = false) String parentJoinColumn,
            @McpToolParam(description = "Child table join column name", required = false) String childJoinColumn,
            @McpToolParam(description = "Custom SQL join expression (for CUSTOM_SQL type)", required = false) String joinExpression,
            @McpToolParam(description = "Join mode: INNER or LEFT", required = false) String joinMode) {
        return ToolExecutor.run("add dataset relation", () ->
                compact(definitionService.addRelation(datasetId, parentNodeCode, childNodeCode,
                        relationType, parentJoinColumn, childJoinColumn, joinExpression,
                        joinMode, null, null, null, null, null)));
    }

    @McpTool(description = "List all relations of a dataset")
    public List<Map<String, Object>> list_dataset_relations(
            @McpToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("list dataset relations", () ->
                definitionService.listRelations(datasetId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Delete a relation from a dataset (only allowed in DRAFT status)")
    public String delete_dataset_relation(
            @McpToolParam(description = "Relation ID") Long relationId) {
        ToolExecutor.runVoid("delete dataset relation", () ->
                definitionService.deleteRelation(relationId));
        return "Relation " + relationId + " deleted successfully";
    }

    // ===== Filters =====

    @McpTool(description = "Add a filter condition to a dataset node. Filters are WHERE clause fragments " +
            "that can be parameterized with ${paramName} placeholders.")
    public Map<String, Object> add_dataset_filter(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "Node code to apply filter to") String nodeCode,
            @McpToolParam(description = "SQL WHERE clause fragment, e.g. 'status = 1' or 'created_at > ${startDate}'") String filterExpression,
            @McpToolParam(description = "Filter name/description", required = false) String filterName,
            @McpToolParam(description = "Whether filter uses parameters", required = false) Boolean parameterized,
            @McpToolParam(description = "Parameter name if parameterized", required = false) String paramName,
            @McpToolParam(description = "Parameter type: STRING, NUMBER, DATE, LIST", required = false) String paramType,
            @McpToolParam(description = "Default value for parameter", required = false) String defaultValue,
            @McpToolParam(description = "Whether parameter is required", required = false) Boolean required) {
        return ToolExecutor.run("add dataset filter", () ->
                compact(definitionService.addFilter(datasetId, nodeCode, filterName, filterExpression,
                        parameterized, paramName, paramType, defaultValue, required, null, null)));
    }

    @McpTool(description = "List filter conditions of a dataset, optionally filtered by node code")
    public List<Map<String, Object>> list_dataset_filters(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "Node code filter", required = false) String nodeCode) {
        return ToolExecutor.run("list dataset filters", () ->
                definitionService.listFilters(datasetId, nodeCode).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    // ===== Field Mappings =====

    @McpTool(description = "Add a field mapping to a dataset node. Maps a source column to an output field " +
            "with optional transformation (via transform rule or inline expression).")
    public Map<String, Object> add_dataset_field_mapping(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "Node code") String nodeCode,
            @McpToolParam(description = "Source column name") String sourceField,
            @McpToolParam(description = "Output field name in result") String outputField,
            @McpToolParam(description = "Output type: STRING, NUMBER, DATE, BOOLEAN, JSON", required = false) String outputType,
            @McpToolParam(description = "Transform rule ID (optional)", required = false) Long transformRuleId,
            @McpToolParam(description = "Inline SQL expression (optional)", required = false) String inlineExpression,
            @McpToolParam(description = "Default value if null", required = false) String defaultValue) {
        return ToolExecutor.run("add dataset field mapping", () ->
                compact(definitionService.addFieldMapping(datasetId, nodeCode, sourceField, outputField,
                        outputType, transformRuleId, inlineExpression, defaultValue,
                        null, null, null)));
    }

    @McpTool(description = "List field mappings of a dataset, optionally filtered by node code")
    public List<Map<String, Object>> list_dataset_field_mappings(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "Node code filter", required = false) String nodeCode) {
        return ToolExecutor.run("list dataset field mappings", () ->
                definitionService.listFieldMappings(datasetId, nodeCode).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    // ===== Execution =====

    @McpTool(description = "Execute a PUBLISHED dataset asynchronously. Connects to the target datasource, " +
            "queries each node in topological order, applies transforms, and assembles nested JSON output.")
    public Map<String, Object> execute_dataset(
            @McpToolParam(description = "Dataset ID (must be PUBLISHED)") Long datasetId,
            @McpToolParam(description = "Root key value to filter the root node", required = false) String rootKeyValue,
            @McpToolParam(description = "Runtime parameters as JSON object string, e.g. {\"startDate\":\"2024-01-01\"}", required = false) String paramsJson) {
        return ToolExecutor.run("execute dataset", () -> {
            Map<String, String> params = null;
            if (paramsJson != null && !paramsJson.isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper =
                            new com.fasterxml.jackson.databind.ObjectMapper();
                    params = mapper.readValue(paramsJson,
                            new com.fasterxml.jackson.core.type.TypeReference<>() {});
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid params JSON: " + e.getMessage());
                }
            }
            return compact(executionService.executeDataset(datasetId, rootKeyValue, params));
        });
    }

    @McpTool(description = "Get dataset execution instance details including status, progress and statistics")
    public Map<String, Object> get_dataset_instance(
            @McpToolParam(description = "Instance ID") Long instanceId) {
        return ToolExecutor.run("get dataset instance", () ->
                compact(executionService.getInstance(instanceId)));
    }

    @McpTool(description = "List execution instances for a dataset, optionally filtered by status")
    public List<Map<String, Object>> list_dataset_instances(
            @McpToolParam(description = "Dataset ID") Long datasetId,
            @McpToolParam(description = "Status filter: PENDING, RUNNING, SUCCESS, PARTIAL_SUCCESS, FAILED", required = false) String status) {
        return ToolExecutor.run("list dataset instances", () ->
                executionService.listInstances(datasetId, status).stream()
                        .map(McpResponseHelper::compact).toList());
    }
}
