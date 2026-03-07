package com.rock.metadata.controller;

import com.rock.metadata.dto.*;
import com.rock.metadata.model.*;
import com.rock.metadata.service.DatasetDefinitionService;
import com.rock.metadata.service.DatasetExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetDefinitionService definitionService;
    private final DatasetExecutionService executionService;

    // ===== Dataset Definition =====

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetDefinition create(@Valid @RequestBody DatasetDefinitionRequest req) {
        return definitionService.createDataset(req.getDatasetCode(), req.getDatasetName(),
                req.getDescription(), req.getBusinessDomain(), req.getDatasourceId(),
                req.getOutputFormat(), req.getRootNodeCode(), req.getMaxExecutionTimeSeconds(),
                req.getOwner());
    }

    @GetMapping
    public List<DatasetDefinition> list(
            @RequestParam(required = false) Long datasourceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String domain) {
        return definitionService.listDatasets(datasourceId, status, domain);
    }

    @GetMapping("/{id}")
    public DatasetDefinition get(@PathVariable Long id) {
        return definitionService.getDataset(id);
    }

    @GetMapping("/by-code/{code}")
    public DatasetDefinition getByCode(@PathVariable String code) {
        return definitionService.getDatasetByCode(code);
    }

    @GetMapping("/{id}/detail")
    public DatasetDetailResponse getDetail(@PathVariable Long id) {
        return definitionService.getDatasetDetail(id);
    }

    @PutMapping("/{id}")
    public DatasetDefinition update(@PathVariable Long id,
                                     @RequestBody DatasetDefinitionRequest req) {
        return definitionService.updateDataset(id, req.getDatasetName(), req.getDescription(),
                req.getBusinessDomain(), req.getOutputFormat(), req.getRootNodeCode(),
                req.getMaxExecutionTimeSeconds(), req.getOwner());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        definitionService.deleteDataset(id);
    }

    @PostMapping("/{id}/publish")
    public DatasetDefinition publish(@PathVariable Long id) {
        return definitionService.publishDataset(id);
    }

    @PostMapping("/{id}/archive")
    public DatasetDefinition archive(@PathVariable Long id) {
        return definitionService.archiveDataset(id);
    }

    @PostMapping("/{id}/validate")
    public DatasetValidationResponse validate(@PathVariable Long id) {
        return definitionService.validateDataset(id);
    }

    // ===== Nodes =====

    @PostMapping("/nodes")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetNode addNode(@Valid @RequestBody DatasetNodeRequest req) {
        return definitionService.addNode(req.getDatasetId(), req.getNodeCode(), req.getNodeName(),
                req.getSourceSchema(), req.getSourceTable(), req.getNodeType(),
                req.getParentNodeCode(), req.getExecutionOrder(), req.getCardinality(),
                req.getMaxRows(), req.getSource(), req.getConfidence(), req.getEnabled());
    }

    @GetMapping("/{datasetId}/nodes")
    public List<DatasetNode> listNodes(@PathVariable Long datasetId) {
        return definitionService.listNodes(datasetId);
    }

    @PutMapping("/nodes/{nodeId}")
    public DatasetNode updateNode(@PathVariable Long nodeId,
                                   @RequestBody DatasetNodeRequest req) {
        return definitionService.updateNode(nodeId, req.getNodeName(), req.getSourceSchema(),
                req.getSourceTable(), req.getNodeType(), req.getParentNodeCode(),
                req.getExecutionOrder(), req.getCardinality(), req.getMaxRows(), req.getEnabled());
    }

    @DeleteMapping("/nodes/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable Long nodeId) {
        definitionService.deleteNode(nodeId);
    }

    // ===== Relations =====

    @PostMapping("/relations")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetNodeRelation addRelation(@Valid @RequestBody DatasetNodeRelationRequest req) {
        return definitionService.addRelation(req.getDatasetId(), req.getParentNodeCode(),
                req.getChildNodeCode(), req.getRelationType(), req.getParentJoinColumn(),
                req.getChildJoinColumn(), req.getJoinExpression(), req.getJoinMode(),
                req.getDependencyLevel(), req.getMaxDepth(), req.getSource(),
                req.getConfidence(), req.getEnabled());
    }

    @GetMapping("/{datasetId}/relations")
    public List<DatasetNodeRelation> listRelations(@PathVariable Long datasetId) {
        return definitionService.listRelations(datasetId);
    }

    @PutMapping("/relations/{id}")
    public DatasetNodeRelation updateRelation(@PathVariable Long id,
                                               @RequestBody DatasetNodeRelationRequest req) {
        return definitionService.updateRelation(id, req.getRelationType(),
                req.getParentJoinColumn(), req.getChildJoinColumn(),
                req.getJoinExpression(), req.getJoinMode(), req.getEnabled());
    }

    @DeleteMapping("/relations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRelation(@PathVariable Long id) {
        definitionService.deleteRelation(id);
    }

    // ===== Filters =====

    @PostMapping("/filters")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetNodeFilter addFilter(@Valid @RequestBody DatasetNodeFilterRequest req) {
        return definitionService.addFilter(req.getDatasetId(), req.getNodeCode(),
                req.getFilterName(), req.getFilterExpression(), req.getParameterized(),
                req.getParamName(), req.getParamType(), req.getDefaultValue(),
                req.getRequired(), req.getSortOrder(), req.getEnabled());
    }

    @GetMapping("/{datasetId}/filters")
    public List<DatasetNodeFilter> listFilters(@PathVariable Long datasetId,
                                                @RequestParam(required = false) String nodeCode) {
        return definitionService.listFilters(datasetId, nodeCode);
    }

    @PutMapping("/filters/{id}")
    public DatasetNodeFilter updateFilter(@PathVariable Long id,
                                           @RequestBody DatasetNodeFilterRequest req) {
        return definitionService.updateFilter(id, req.getFilterName(), req.getFilterExpression(),
                req.getParameterized(), req.getParamName(), req.getParamType(),
                req.getDefaultValue(), req.getRequired(), req.getSortOrder(), req.getEnabled());
    }

    @DeleteMapping("/filters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFilter(@PathVariable Long id) {
        definitionService.deleteFilter(id);
    }

    // ===== Field Mappings =====

    @PostMapping("/field-mappings")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetFieldMapping addFieldMapping(@Valid @RequestBody DatasetFieldMappingRequest req) {
        return definitionService.addFieldMapping(req.getDatasetId(), req.getNodeCode(),
                req.getSourceField(), req.getOutputField(), req.getOutputType(),
                req.getTransformRuleId(), req.getInlineExpression(), req.getDefaultValue(),
                req.getRequired(), req.getSortOrder(), req.getEnabled());
    }

    @GetMapping("/{datasetId}/field-mappings")
    public List<DatasetFieldMapping> listFieldMappings(@PathVariable Long datasetId,
                                                        @RequestParam(required = false) String nodeCode) {
        return definitionService.listFieldMappings(datasetId, nodeCode);
    }

    @PutMapping("/field-mappings/{id}")
    public DatasetFieldMapping updateFieldMapping(@PathVariable Long id,
                                                   @RequestBody DatasetFieldMappingRequest req) {
        return definitionService.updateFieldMapping(id, req.getSourceField(), req.getOutputField(),
                req.getOutputType(), req.getTransformRuleId(), req.getInlineExpression(),
                req.getDefaultValue(), req.getRequired(), req.getSortOrder(), req.getEnabled());
    }

    @DeleteMapping("/field-mappings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFieldMapping(@PathVariable Long id) {
        definitionService.deleteFieldMapping(id);
    }

    // ===== Transform Rules (global) =====

    @PostMapping("/transform-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetTransformRule createTransformRule(@Valid @RequestBody DatasetTransformRuleRequest req) {
        return definitionService.createTransformRule(req.getRuleCode(), req.getRuleName(),
                req.getRuleType(), req.getRuleContent(), req.getDescription(), req.getActive());
    }

    @GetMapping("/transform-rules")
    public List<DatasetTransformRule> listTransformRules(
            @RequestParam(required = false) Boolean activeOnly) {
        return definitionService.listTransformRules(activeOnly);
    }

    @GetMapping("/transform-rules/{id}")
    public DatasetTransformRule getTransformRule(@PathVariable Long id) {
        return definitionService.getTransformRule(id);
    }

    @PutMapping("/transform-rules/{id}")
    public DatasetTransformRule updateTransformRule(@PathVariable Long id,
                                                     @RequestBody DatasetTransformRuleRequest req) {
        return definitionService.updateTransformRule(id, req.getRuleName(), req.getRuleType(),
                req.getRuleContent(), req.getDescription(), req.getActive());
    }

    @DeleteMapping("/transform-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransformRule(@PathVariable Long id) {
        definitionService.deleteTransformRule(id);
    }

    // ===== Execution =====

    @PostMapping("/{id}/execute")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DatasetInstance execute(@PathVariable Long id,
                                   @RequestBody(required = false) DatasetExecuteRequest req) {
        String rootKeyValue = req != null ? req.getRootKeyValue() : null;
        var params = req != null ? req.getParams() : null;
        return executionService.executeDataset(id, rootKeyValue, params);
    }

    @GetMapping("/instances")
    public List<DatasetInstance> listInstances(
            @RequestParam Long datasetId,
            @RequestParam(required = false) String status) {
        return executionService.listInstances(datasetId, status);
    }

    @GetMapping("/instances/{id}")
    public DatasetInstance getInstance(@PathVariable Long id) {
        return executionService.getInstance(id);
    }

    @GetMapping("/instances/{id}/detail")
    public DatasetInstanceDetailResponse getInstanceDetail(@PathVariable Long id) {
        return executionService.getInstanceDetail(id);
    }

    @GetMapping("/instances/{id}/snapshot")
    public DatasetInstanceSnapshot getAggregateSnapshot(@PathVariable Long id) {
        return executionService.getSnapshot(id, null);
    }

    @GetMapping("/instances/{id}/snapshot/{nodeCode}")
    public DatasetInstanceSnapshot getNodeSnapshot(@PathVariable Long id,
                                                    @PathVariable String nodeCode) {
        return executionService.getSnapshot(id, nodeCode);
    }

    @DeleteMapping("/instances/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInstance(@PathVariable Long id) {
        executionService.deleteInstance(id);
    }
}
