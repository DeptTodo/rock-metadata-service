package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.DatasetDetailResponse;
import com.rock.metadata.dto.DatasetInstanceDetailResponse;
import com.rock.metadata.dto.DatasetValidationResponse;
import com.rock.metadata.model.*;
import com.rock.metadata.service.DatasetDefinitionService;
import com.rock.metadata.service.DatasetExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatasetTools {

    private final DatasetDefinitionService definitionService;
    private final DatasetExecutionService executionService;

    // ===== Dataset Definition =====

    @Tool(description = "Create a new dataset definition. A dataset defines a reusable template " +
            "with aggregation nodes, relations, field mappings and transform rules.")
    public DatasetDefinition create_dataset(
            @ToolParam(description = "Unique dataset code") String datasetCode,
            @ToolParam(description = "Display name") String datasetName,
            @ToolParam(description = "Target datasource ID") Long datasourceId,
            @ToolParam(description = "Description", required = false) String description,
            @ToolParam(description = "Business domain", required = false) String businessDomain,
            @ToolParam(description = "Output format: TREE or FLAT", required = false) String outputFormat,
            @ToolParam(description = "Root node code", required = false) String rootNodeCode,
            @ToolParam(description = "Max execution timeout in seconds", required = false) Integer maxExecutionTimeSeconds,
            @ToolParam(description = "Owner", required = false) String owner) {
        return ToolExecutor.run("create dataset", () ->
                definitionService.createDataset(datasetCode, datasetName, description,
                        businessDomain, datasourceId, outputFormat, rootNodeCode,
                        maxExecutionTimeSeconds, owner));
    }

    @Tool(description = "List dataset definitions, optionally filtered by datasourceId, status (DRAFT/PUBLISHED/ARCHIVED), or domain")
    public List<DatasetDefinition> list_datasets(
            @ToolParam(description = "Datasource ID filter", required = false) Long datasourceId,
            @ToolParam(description = "Status filter: DRAFT, PUBLISHED, ARCHIVED", required = false) String status,
            @ToolParam(description = "Business domain filter", required = false) String domain) {
        return ToolExecutor.run("list datasets", () ->
                definitionService.listDatasets(datasourceId, status, domain));
    }

    @Tool(description = "Get complete dataset definition detail including all nodes, relations, filters and field mappings")
    public DatasetDetailResponse get_dataset_detail(
            @ToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("get dataset detail", () ->
                definitionService.getDatasetDetail(datasetId));
    }

    @Tool(description = "Update a dataset definition (only allowed in DRAFT status)")
    public DatasetDefinition update_dataset(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "New display name", required = false) String datasetName,
            @ToolParam(description = "New description", required = false) String description,
            @ToolParam(description = "Business domain", required = false) String businessDomain,
            @ToolParam(description = "Output format: TREE or FLAT", required = false) String outputFormat,
            @ToolParam(description = "Root node code", required = false) String rootNodeCode,
            @ToolParam(description = "Max execution timeout", required = false) Integer maxExecutionTimeSeconds,
            @ToolParam(description = "Owner", required = false) String owner) {
        return ToolExecutor.run("update dataset", () ->
                definitionService.updateDataset(datasetId, datasetName, description,
                        businessDomain, outputFormat, rootNodeCode,
                        maxExecutionTimeSeconds, owner));
    }

    @Tool(description = "Delete a dataset definition and all its nodes, relations, filters, field mappings and instances")
    public String delete_dataset(
            @ToolParam(description = "Dataset ID") Long datasetId) {
        ToolExecutor.runVoid("delete dataset", () -> definitionService.deleteDataset(datasetId));
        return "Dataset " + datasetId + " deleted successfully";
    }

    @Tool(description = "Publish a dataset (DRAFT -> PUBLISHED). Validates the dataset before publishing. " +
            "Only PUBLISHED datasets can be executed.")
    public DatasetDefinition publish_dataset(
            @ToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("publish dataset", () ->
                definitionService.publishDataset(datasetId));
    }

    @Tool(description = "Validate a dataset definition. Checks root node, parent references, " +
            "relation references, field mappings, and detects circular dependencies using Kahn's algorithm.")
    public DatasetValidationResponse validate_dataset(
            @ToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("validate dataset", () ->
                definitionService.validateDataset(datasetId));
    }

    // ===== Nodes =====

    @Tool(description = "Add a node to a dataset. Nodes represent source tables in the aggregation tree. " +
            "Node types: ROOT, CHILD_OBJECT, CHILD_LIST, BRIDGE, DERIVED.")
    public DatasetNode add_dataset_node(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "Unique node code within the dataset") String nodeCode,
            @ToolParam(description = "Display name") String nodeName,
            @ToolParam(description = "Source table name") String sourceTable,
            @ToolParam(description = "Node type: ROOT, CHILD_OBJECT, CHILD_LIST, BRIDGE, DERIVED") String nodeType,
            @ToolParam(description = "Source schema name", required = false) String sourceSchema,
            @ToolParam(description = "Parent node code (null for ROOT)", required = false) String parentNodeCode,
            @ToolParam(description = "Execution order (lower = earlier)", required = false) Integer executionOrder,
            @ToolParam(description = "Cardinality: ONE_TO_ONE, ONE_TO_MANY, MANY_TO_MANY", required = false) String cardinality,
            @ToolParam(description = "Max rows per node", required = false) Integer maxRows) {
        return ToolExecutor.run("add dataset node", () ->
                definitionService.addNode(datasetId, nodeCode, nodeName, sourceSchema,
                        sourceTable, nodeType, parentNodeCode, executionOrder, cardinality,
                        maxRows, null, null, null));
    }

    @Tool(description = "List all nodes of a dataset, ordered by execution order")
    public List<DatasetNode> list_dataset_nodes(
            @ToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("list dataset nodes", () ->
                definitionService.listNodes(datasetId));
    }

    @Tool(description = "Delete a node from a dataset (only allowed in DRAFT status)")
    public String delete_dataset_node(
            @ToolParam(description = "Node ID") Long nodeId) {
        ToolExecutor.runVoid("delete dataset node", () -> definitionService.deleteNode(nodeId));
        return "Node " + nodeId + " deleted successfully";
    }

    // ===== Relations =====

    @Tool(description = "Add a relation between two nodes. Relation types: FK (foreign key lookup), " +
            "COLUMN_MATCH (explicit column match), CUSTOM_SQL (custom join expression).")
    public DatasetNodeRelation add_dataset_relation(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "Parent node code") String parentNodeCode,
            @ToolParam(description = "Child node code") String childNodeCode,
            @ToolParam(description = "Relation type: FK, COLUMN_MATCH, CUSTOM_SQL", required = false) String relationType,
            @ToolParam(description = "Parent table join column name", required = false) String parentJoinColumn,
            @ToolParam(description = "Child table join column name", required = false) String childJoinColumn,
            @ToolParam(description = "Custom SQL join expression (for CUSTOM_SQL type)", required = false) String joinExpression,
            @ToolParam(description = "Join mode: INNER or LEFT", required = false) String joinMode) {
        return ToolExecutor.run("add dataset relation", () ->
                definitionService.addRelation(datasetId, parentNodeCode, childNodeCode,
                        relationType, parentJoinColumn, childJoinColumn, joinExpression,
                        joinMode, null, null, null, null, null));
    }

    @Tool(description = "List all relations of a dataset")
    public List<DatasetNodeRelation> list_dataset_relations(
            @ToolParam(description = "Dataset ID") Long datasetId) {
        return ToolExecutor.run("list dataset relations", () ->
                definitionService.listRelations(datasetId));
    }

    @Tool(description = "Delete a relation from a dataset (only allowed in DRAFT status)")
    public String delete_dataset_relation(
            @ToolParam(description = "Relation ID") Long relationId) {
        ToolExecutor.runVoid("delete dataset relation", () ->
                definitionService.deleteRelation(relationId));
        return "Relation " + relationId + " deleted successfully";
    }

    // ===== Filters =====

    @Tool(description = "Add a filter condition to a dataset node. Filters are WHERE clause fragments " +
            "that can be parameterized with ${paramName} placeholders.")
    public DatasetNodeFilter add_dataset_filter(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "Node code to apply filter to") String nodeCode,
            @ToolParam(description = "SQL WHERE clause fragment, e.g. 'status = 1' or 'created_at > ${startDate}'") String filterExpression,
            @ToolParam(description = "Filter name/description", required = false) String filterName,
            @ToolParam(description = "Whether filter uses parameters", required = false) Boolean parameterized,
            @ToolParam(description = "Parameter name if parameterized", required = false) String paramName,
            @ToolParam(description = "Parameter type: STRING, NUMBER, DATE, LIST", required = false) String paramType,
            @ToolParam(description = "Default value for parameter", required = false) String defaultValue,
            @ToolParam(description = "Whether parameter is required", required = false) Boolean required) {
        return ToolExecutor.run("add dataset filter", () ->
                definitionService.addFilter(datasetId, nodeCode, filterName, filterExpression,
                        parameterized, paramName, paramType, defaultValue, required, null, null));
    }

    @Tool(description = "List filter conditions of a dataset, optionally filtered by node code")
    public List<DatasetNodeFilter> list_dataset_filters(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "Node code filter", required = false) String nodeCode) {
        return ToolExecutor.run("list dataset filters", () ->
                definitionService.listFilters(datasetId, nodeCode));
    }

    // ===== Field Mappings =====

    @Tool(description = "Add a field mapping to a dataset node. Maps a source column to an output field " +
            "with optional transformation (via transform rule or inline expression).")
    public DatasetFieldMapping add_dataset_field_mapping(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "Node code") String nodeCode,
            @ToolParam(description = "Source column name") String sourceField,
            @ToolParam(description = "Output field name in result") String outputField,
            @ToolParam(description = "Output type: STRING, NUMBER, DATE, BOOLEAN, JSON", required = false) String outputType,
            @ToolParam(description = "Transform rule ID (optional)", required = false) Long transformRuleId,
            @ToolParam(description = "Inline SQL expression (optional)", required = false) String inlineExpression,
            @ToolParam(description = "Default value if null", required = false) String defaultValue) {
        return ToolExecutor.run("add dataset field mapping", () ->
                definitionService.addFieldMapping(datasetId, nodeCode, sourceField, outputField,
                        outputType, transformRuleId, inlineExpression, defaultValue,
                        null, null, null));
    }

    @Tool(description = "List field mappings of a dataset, optionally filtered by node code")
    public List<DatasetFieldMapping> list_dataset_field_mappings(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "Node code filter", required = false) String nodeCode) {
        return ToolExecutor.run("list dataset field mappings", () ->
                definitionService.listFieldMappings(datasetId, nodeCode));
    }

    // ===== Execution =====

    @Tool(description = "Execute a PUBLISHED dataset asynchronously. Connects to the target datasource, " +
            "queries each node in topological order, applies transforms, and assembles nested JSON output.")
    public DatasetInstance execute_dataset(
            @ToolParam(description = "Dataset ID (must be PUBLISHED)") Long datasetId,
            @ToolParam(description = "Root key value to filter the root node", required = false) String rootKeyValue,
            @ToolParam(description = "Runtime parameters as JSON object string, e.g. {\"startDate\":\"2024-01-01\"}", required = false) String paramsJson) {
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
            return executionService.executeDataset(datasetId, rootKeyValue, params);
        });
    }

    @Tool(description = "Get dataset execution instance details including status, progress and statistics")
    public DatasetInstance get_dataset_instance(
            @ToolParam(description = "Instance ID") Long instanceId) {
        return ToolExecutor.run("get dataset instance", () ->
                executionService.getInstance(instanceId));
    }

    @Tool(description = "List execution instances for a dataset, optionally filtered by status")
    public List<DatasetInstance> list_dataset_instances(
            @ToolParam(description = "Dataset ID") Long datasetId,
            @ToolParam(description = "Status filter: PENDING, RUNNING, SUCCESS, PARTIAL_SUCCESS, FAILED", required = false) String status) {
        return ToolExecutor.run("list dataset instances", () ->
                executionService.listInstances(datasetId, status));
    }
}
