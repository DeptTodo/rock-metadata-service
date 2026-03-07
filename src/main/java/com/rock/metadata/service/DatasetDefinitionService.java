package com.rock.metadata.service;

import com.rock.metadata.dto.DatasetDetailResponse;
import com.rock.metadata.dto.DatasetValidationResponse;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetDefinitionService {

    private final DatasetDefinitionRepository definitionRepository;
    private final DatasetNodeRepository nodeRepository;
    private final DatasetNodeRelationRepository relationRepository;
    private final DatasetNodeFilterRepository filterRepository;
    private final DatasetFieldMappingRepository fieldMappingRepository;
    private final DatasetTransformRuleRepository transformRuleRepository;
    private final DatasetInstanceRepository instanceRepository;
    private final DatasetInstanceSnapshotRepository snapshotRepository;
    private final DataSourceConfigRepository dataSourceConfigRepository;

    // ===== Dataset Definition CRUD =====

    @Transactional
    public DatasetDefinition createDataset(String datasetCode, String datasetName, String description,
                                            String businessDomain, Long datasourceId, String outputFormat,
                                            String rootNodeCode, Integer maxExecutionTimeSeconds, String owner) {
        if (definitionRepository.existsByDatasetCode(datasetCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dataset code already exists: " + datasetCode);
        }
        if (!dataSourceConfigRepository.existsById(datasourceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "DataSource not found: " + datasourceId);
        }
        DatasetDefinition def = new DatasetDefinition();
        def.setDatasetCode(datasetCode);
        def.setDatasetName(datasetName);
        def.setDescription(description);
        def.setBusinessDomain(businessDomain);
        def.setDatasourceId(datasourceId);
        if (outputFormat != null) def.setOutputFormat(outputFormat);
        def.setRootNodeCode(rootNodeCode);
        if (maxExecutionTimeSeconds != null) def.setMaxExecutionTimeSeconds(maxExecutionTimeSeconds);
        def.setOwner(owner);
        return definitionRepository.save(def);
    }

    @Transactional(readOnly = true)
    public DatasetDefinition getDataset(Long id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dataset not found: " + id));
    }

    @Transactional(readOnly = true)
    public DatasetDefinition getDatasetByCode(String code) {
        return definitionRepository.findByDatasetCode(code)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dataset not found: " + code));
    }

    @Transactional(readOnly = true)
    public List<DatasetDefinition> listDatasets(Long datasourceId, String status, String domain) {
        if (datasourceId != null && status != null) {
            return definitionRepository.findByDatasourceIdAndStatus(
                    datasourceId, DatasetStatus.valueOf(status));
        }
        if (datasourceId != null) {
            return definitionRepository.findByDatasourceId(datasourceId);
        }
        if (status != null) {
            return definitionRepository.findByStatus(DatasetStatus.valueOf(status));
        }
        if (domain != null) {
            return definitionRepository.findByBusinessDomain(domain);
        }
        return definitionRepository.findAll();
    }

    @Transactional
    public DatasetDefinition updateDataset(Long id, String datasetName, String description,
                                            String businessDomain, String outputFormat,
                                            String rootNodeCode, Integer maxExecutionTimeSeconds, String owner) {
        DatasetDefinition def = getDataset(id);
        if (def.getStatus() != DatasetStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Can only update datasets in DRAFT status");
        }
        if (datasetName != null) def.setDatasetName(datasetName);
        if (description != null) def.setDescription(description);
        if (businessDomain != null) def.setBusinessDomain(businessDomain);
        if (outputFormat != null) def.setOutputFormat(outputFormat);
        if (rootNodeCode != null) def.setRootNodeCode(rootNodeCode);
        if (maxExecutionTimeSeconds != null) def.setMaxExecutionTimeSeconds(maxExecutionTimeSeconds);
        if (owner != null) def.setOwner(owner);
        return definitionRepository.save(def);
    }

    @Transactional
    public void deleteDataset(Long id) {
        if (!definitionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dataset not found: " + id);
        }
        // Cascade delete snapshots via instances
        List<DatasetInstance> instances = instanceRepository.findByDatasetIdOrderByCreatedAtDesc(id);
        for (DatasetInstance inst : instances) {
            snapshotRepository.deleteByInstanceId(inst.getId());
        }
        instanceRepository.deleteByDatasetId(id);
        fieldMappingRepository.deleteByDatasetId(id);
        filterRepository.deleteByDatasetId(id);
        relationRepository.deleteByDatasetId(id);
        nodeRepository.deleteByDatasetId(id);
        definitionRepository.deleteById(id);
    }

    @Transactional
    public DatasetDefinition publishDataset(Long id) {
        DatasetDefinition def = getDataset(id);
        if (def.getStatus() != DatasetStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Can only publish datasets in DRAFT status");
        }
        DatasetValidationResponse validation = doValidate(def);
        if (!validation.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dataset validation failed: " + String.join("; ", validation.getErrors()));
        }
        def.setStatus(DatasetStatus.PUBLISHED);
        def.setVersion(def.getVersion() + 1);
        return definitionRepository.save(def);
    }

    @Transactional
    public DatasetDefinition archiveDataset(Long id) {
        DatasetDefinition def = getDataset(id);
        if (def.getStatus() != DatasetStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Can only archive datasets in PUBLISHED status");
        }
        def.setStatus(DatasetStatus.ARCHIVED);
        return definitionRepository.save(def);
    }

    @Transactional(readOnly = true)
    public DatasetDetailResponse getDatasetDetail(Long id) {
        DatasetDefinition def = getDataset(id);
        DatasetDetailResponse resp = new DatasetDetailResponse();
        resp.setDefinition(def);
        resp.setNodes(nodeRepository.findByDatasetIdOrderByExecutionOrder(id));
        resp.setRelations(relationRepository.findByDatasetId(id));
        resp.setFilters(filterRepository.findByDatasetId(id));
        resp.setFieldMappings(fieldMappingRepository.findByDatasetId(id));
        return resp;
    }

    @Transactional(readOnly = true)
    public DatasetValidationResponse validateDataset(Long id) {
        DatasetDefinition def = getDataset(id);
        return doValidate(def);
    }

    // ===== Nodes =====

    @Transactional
    public DatasetNode addNode(Long datasetId, String nodeCode, String nodeName,
                                String sourceSchema, String sourceTable, String nodeType,
                                String parentNodeCode, Integer executionOrder, String cardinality,
                                Integer maxRows, String source, Double confidence, Boolean enabled) {
        DatasetDefinition def = getDataset(datasetId);
        checkDraft(def);
        if (nodeRepository.findByDatasetIdAndNodeCode(datasetId, nodeCode).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Node code already exists in this dataset: " + nodeCode);
        }
        DatasetNode node = new DatasetNode();
        node.setDatasetId(datasetId);
        node.setNodeCode(nodeCode);
        node.setNodeName(nodeName);
        node.setSourceSchema(sourceSchema);
        node.setSourceTable(sourceTable);
        node.setNodeType(nodeType);
        node.setParentNodeCode(parentNodeCode);
        if (executionOrder != null) node.setExecutionOrder(executionOrder);
        node.setCardinality(cardinality);
        if (maxRows != null) node.setMaxRows(maxRows);
        if (source != null) node.setSource(source);
        node.setConfidence(confidence);
        if (enabled != null) node.setEnabled(enabled);
        return nodeRepository.save(node);
    }

    @Transactional(readOnly = true)
    public List<DatasetNode> listNodes(Long datasetId) {
        return nodeRepository.findByDatasetIdOrderByExecutionOrder(datasetId);
    }

    @Transactional
    public DatasetNode updateNode(Long nodeId, String nodeName, String sourceSchema,
                                   String sourceTable, String nodeType, String parentNodeCode,
                                   Integer executionOrder, String cardinality, Integer maxRows,
                                   Boolean enabled) {
        DatasetNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node not found: " + nodeId));
        checkDraft(getDataset(node.getDatasetId()));
        if (nodeName != null) node.setNodeName(nodeName);
        if (sourceSchema != null) node.setSourceSchema(sourceSchema);
        if (sourceTable != null) node.setSourceTable(sourceTable);
        if (nodeType != null) node.setNodeType(nodeType);
        if (parentNodeCode != null) node.setParentNodeCode(parentNodeCode);
        if (executionOrder != null) node.setExecutionOrder(executionOrder);
        if (cardinality != null) node.setCardinality(cardinality);
        if (maxRows != null) node.setMaxRows(maxRows);
        if (enabled != null) node.setEnabled(enabled);
        return nodeRepository.save(node);
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        DatasetNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node not found: " + nodeId));
        checkDraft(getDataset(node.getDatasetId()));
        nodeRepository.deleteById(nodeId);
    }

    // ===== Relations =====

    @Transactional
    public DatasetNodeRelation addRelation(Long datasetId, String parentNodeCode,
                                            String childNodeCode, String relationType,
                                            String parentJoinColumn, String childJoinColumn,
                                            String joinExpression, String joinMode,
                                            Integer dependencyLevel, Integer maxDepth,
                                            String source, Double confidence, Boolean enabled) {
        DatasetDefinition def = getDataset(datasetId);
        checkDraft(def);
        DatasetNodeRelation rel = new DatasetNodeRelation();
        rel.setDatasetId(datasetId);
        rel.setParentNodeCode(parentNodeCode);
        rel.setChildNodeCode(childNodeCode);
        rel.setRelationType(relationType);
        rel.setParentJoinColumn(parentJoinColumn);
        rel.setChildJoinColumn(childJoinColumn);
        rel.setJoinExpression(joinExpression);
        if (joinMode != null) rel.setJoinMode(joinMode);
        if (dependencyLevel != null) rel.setDependencyLevel(dependencyLevel);
        if (maxDepth != null) rel.setMaxDepth(maxDepth);
        if (source != null) rel.setSource(source);
        rel.setConfidence(confidence);
        if (enabled != null) rel.setEnabled(enabled);
        return relationRepository.save(rel);
    }

    @Transactional(readOnly = true)
    public List<DatasetNodeRelation> listRelations(Long datasetId) {
        return relationRepository.findByDatasetId(datasetId);
    }

    @Transactional
    public DatasetNodeRelation updateRelation(Long id, String relationType,
                                               String parentJoinColumn, String childJoinColumn,
                                               String joinExpression, String joinMode,
                                               Boolean enabled) {
        DatasetNodeRelation rel = relationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Relation not found: " + id));
        checkDraft(getDataset(rel.getDatasetId()));
        if (relationType != null) rel.setRelationType(relationType);
        if (parentJoinColumn != null) rel.setParentJoinColumn(parentJoinColumn);
        if (childJoinColumn != null) rel.setChildJoinColumn(childJoinColumn);
        if (joinExpression != null) rel.setJoinExpression(joinExpression);
        if (joinMode != null) rel.setJoinMode(joinMode);
        if (enabled != null) rel.setEnabled(enabled);
        return relationRepository.save(rel);
    }

    @Transactional
    public void deleteRelation(Long id) {
        DatasetNodeRelation rel = relationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Relation not found: " + id));
        checkDraft(getDataset(rel.getDatasetId()));
        relationRepository.deleteById(id);
    }

    // ===== Filters =====

    @Transactional
    public DatasetNodeFilter addFilter(Long datasetId, String nodeCode, String filterName,
                                        String filterExpression, Boolean parameterized,
                                        String paramName, String paramType, String defaultValue,
                                        Boolean required, Integer sortOrder, Boolean enabled) {
        DatasetDefinition def = getDataset(datasetId);
        checkDraft(def);
        DatasetNodeFilter filter = new DatasetNodeFilter();
        filter.setDatasetId(datasetId);
        filter.setNodeCode(nodeCode);
        filter.setFilterName(filterName);
        filter.setFilterExpression(filterExpression);
        if (parameterized != null) filter.setParameterized(parameterized);
        filter.setParamName(paramName);
        filter.setParamType(paramType);
        filter.setDefaultValue(defaultValue);
        if (required != null) filter.setRequired(required);
        if (sortOrder != null) filter.setSortOrder(sortOrder);
        if (enabled != null) filter.setEnabled(enabled);
        return filterRepository.save(filter);
    }

    @Transactional(readOnly = true)
    public List<DatasetNodeFilter> listFilters(Long datasetId, String nodeCode) {
        if (nodeCode != null) {
            return filterRepository.findByDatasetIdAndNodeCodeOrderBySortOrder(datasetId, nodeCode);
        }
        return filterRepository.findByDatasetId(datasetId);
    }

    @Transactional
    public DatasetNodeFilter updateFilter(Long id, String filterName, String filterExpression,
                                           Boolean parameterized, String paramName, String paramType,
                                           String defaultValue, Boolean required, Integer sortOrder,
                                           Boolean enabled) {
        DatasetNodeFilter filter = filterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Filter not found: " + id));
        checkDraft(getDataset(filter.getDatasetId()));
        if (filterName != null) filter.setFilterName(filterName);
        if (filterExpression != null) filter.setFilterExpression(filterExpression);
        if (parameterized != null) filter.setParameterized(parameterized);
        if (paramName != null) filter.setParamName(paramName);
        if (paramType != null) filter.setParamType(paramType);
        if (defaultValue != null) filter.setDefaultValue(defaultValue);
        if (required != null) filter.setRequired(required);
        if (sortOrder != null) filter.setSortOrder(sortOrder);
        if (enabled != null) filter.setEnabled(enabled);
        return filterRepository.save(filter);
    }

    @Transactional
    public void deleteFilter(Long id) {
        DatasetNodeFilter filter = filterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Filter not found: " + id));
        checkDraft(getDataset(filter.getDatasetId()));
        filterRepository.deleteById(id);
    }

    // ===== Field Mappings =====

    @Transactional
    public DatasetFieldMapping addFieldMapping(Long datasetId, String nodeCode, String sourceField,
                                                String outputField, String outputType,
                                                Long transformRuleId, String inlineExpression,
                                                String defaultValue, Boolean required,
                                                Integer sortOrder, Boolean enabled) {
        DatasetDefinition def = getDataset(datasetId);
        checkDraft(def);
        DatasetFieldMapping mapping = new DatasetFieldMapping();
        mapping.setDatasetId(datasetId);
        mapping.setNodeCode(nodeCode);
        mapping.setSourceField(sourceField);
        mapping.setOutputField(outputField);
        mapping.setOutputType(outputType);
        mapping.setTransformRuleId(transformRuleId);
        mapping.setInlineExpression(inlineExpression);
        mapping.setDefaultValue(defaultValue);
        if (required != null) mapping.setRequired(required);
        if (sortOrder != null) mapping.setSortOrder(sortOrder);
        if (enabled != null) mapping.setEnabled(enabled);
        return fieldMappingRepository.save(mapping);
    }

    @Transactional(readOnly = true)
    public List<DatasetFieldMapping> listFieldMappings(Long datasetId, String nodeCode) {
        if (nodeCode != null) {
            return fieldMappingRepository.findByDatasetIdAndNodeCodeOrderBySortOrder(datasetId, nodeCode);
        }
        return fieldMappingRepository.findByDatasetId(datasetId);
    }

    @Transactional
    public DatasetFieldMapping updateFieldMapping(Long id, String sourceField, String outputField,
                                                   String outputType, Long transformRuleId,
                                                   String inlineExpression, String defaultValue,
                                                   Boolean required, Integer sortOrder, Boolean enabled) {
        DatasetFieldMapping mapping = fieldMappingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Field mapping not found: " + id));
        checkDraft(getDataset(mapping.getDatasetId()));
        if (sourceField != null) mapping.setSourceField(sourceField);
        if (outputField != null) mapping.setOutputField(outputField);
        if (outputType != null) mapping.setOutputType(outputType);
        if (transformRuleId != null) mapping.setTransformRuleId(transformRuleId);
        if (inlineExpression != null) mapping.setInlineExpression(inlineExpression);
        if (defaultValue != null) mapping.setDefaultValue(defaultValue);
        if (required != null) mapping.setRequired(required);
        if (sortOrder != null) mapping.setSortOrder(sortOrder);
        if (enabled != null) mapping.setEnabled(enabled);
        return fieldMappingRepository.save(mapping);
    }

    @Transactional
    public void deleteFieldMapping(Long id) {
        DatasetFieldMapping mapping = fieldMappingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Field mapping not found: " + id));
        checkDraft(getDataset(mapping.getDatasetId()));
        fieldMappingRepository.deleteById(id);
    }

    // ===== Transform Rules (global) =====

    @Transactional
    public DatasetTransformRule createTransformRule(String ruleCode, String ruleName,
                                                     String ruleType, String ruleContent,
                                                     String description, Boolean active) {
        if (transformRuleRepository.existsByRuleCode(ruleCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transform rule code already exists: " + ruleCode);
        }
        DatasetTransformRule rule = new DatasetTransformRule();
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleName);
        rule.setRuleType(ruleType);
        rule.setRuleContent(ruleContent);
        rule.setDescription(description);
        if (active != null) rule.setActive(active);
        return transformRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public DatasetTransformRule getTransformRule(Long id) {
        return transformRuleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transform rule not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<DatasetTransformRule> listTransformRules(Boolean activeOnly) {
        if (activeOnly != null && activeOnly) {
            return transformRuleRepository.findByActiveTrue();
        }
        return transformRuleRepository.findAll();
    }

    @Transactional
    public DatasetTransformRule updateTransformRule(Long id, String ruleName, String ruleType,
                                                     String ruleContent, String description,
                                                     Boolean active) {
        DatasetTransformRule rule = getTransformRule(id);
        if (ruleName != null) rule.setRuleName(ruleName);
        if (ruleType != null) rule.setRuleType(ruleType);
        if (ruleContent != null) rule.setRuleContent(ruleContent);
        if (description != null) rule.setDescription(description);
        if (active != null) rule.setActive(active);
        return transformRuleRepository.save(rule);
    }

    @Transactional
    public void deleteTransformRule(Long id) {
        if (!transformRuleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transform rule not found: " + id);
        }
        transformRuleRepository.deleteById(id);
    }

    // ===== Validation =====

    private DatasetValidationResponse doValidate(DatasetDefinition def) {
        DatasetValidationResponse resp = new DatasetValidationResponse();
        Long datasetId = def.getId();
        List<DatasetNode> nodes = nodeRepository.findByDatasetIdOrderByExecutionOrder(datasetId);
        List<DatasetNodeRelation> relations = relationRepository.findByDatasetId(datasetId);

        if (nodes.isEmpty()) {
            resp.getErrors().add("Dataset has no nodes");
            resp.setValid(false);
            return resp;
        }

        // Build node map
        Map<String, DatasetNode> nodeMap = new LinkedHashMap<>();
        for (DatasetNode node : nodes) {
            nodeMap.put(node.getNodeCode(), node);
        }

        // Check root node
        String rootCode = def.getRootNodeCode();
        if (rootCode == null || rootCode.isBlank()) {
            resp.getErrors().add("Root node code is not set on dataset definition");
        } else if (!nodeMap.containsKey(rootCode)) {
            resp.getErrors().add("Root node code '" + rootCode + "' does not reference a valid node");
        } else {
            DatasetNode rootNode = nodeMap.get(rootCode);
            if (!"ROOT".equals(rootNode.getNodeType())) {
                resp.getErrors().add("Root node '" + rootCode + "' has nodeType='"
                        + rootNode.getNodeType() + "', expected ROOT");
            }
        }

        // Check parent references
        for (DatasetNode node : nodes) {
            if (node.getParentNodeCode() != null && !nodeMap.containsKey(node.getParentNodeCode())) {
                resp.getErrors().add("Node '" + node.getNodeCode()
                        + "' references non-existent parent '" + node.getParentNodeCode() + "'");
            }
        }

        // Check relation references
        for (DatasetNodeRelation rel : relations) {
            if (!nodeMap.containsKey(rel.getParentNodeCode())) {
                resp.getErrors().add("Relation references non-existent parent node '"
                        + rel.getParentNodeCode() + "'");
            }
            if (!nodeMap.containsKey(rel.getChildNodeCode())) {
                resp.getErrors().add("Relation references non-existent child node '"
                        + rel.getChildNodeCode() + "'");
            }
        }

        // Check field mapping references
        List<DatasetFieldMapping> mappings = fieldMappingRepository.findByDatasetId(datasetId);
        for (DatasetFieldMapping m : mappings) {
            if (!nodeMap.containsKey(m.getNodeCode())) {
                resp.getErrors().add("Field mapping references non-existent node '"
                        + m.getNodeCode() + "'");
            }
            if (m.getTransformRuleId() != null && !transformRuleRepository.existsById(m.getTransformRuleId())) {
                resp.getErrors().add("Field mapping '" + m.getOutputField()
                        + "' references non-existent transform rule " + m.getTransformRuleId());
            }
        }

        // Kahn's algorithm for cycle detection and topological sort
        List<String> topoOrder = kahnTopologicalSort(nodes, relations, resp);

        if (!resp.getErrors().isEmpty()) {
            resp.setValid(false);
        } else {
            resp.setValid(true);
            resp.setExecutionOrder(topoOrder);
        }
        return resp;
    }

    private List<String> kahnTopologicalSort(List<DatasetNode> nodes,
                                              List<DatasetNodeRelation> relations,
                                              DatasetValidationResponse resp) {
        Set<String> nodeCodes = new LinkedHashSet<>();
        for (DatasetNode n : nodes) {
            if (n.isEnabled()) nodeCodes.add(n.getNodeCode());
        }

        Map<String, Set<String>> adj = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (String code : nodeCodes) {
            adj.put(code, new HashSet<>());
            inDegree.put(code, 0);
        }

        // Build edges from parent->child (both from node.parentNodeCode and from relations)
        for (DatasetNode node : nodes) {
            if (!node.isEnabled()) continue;
            if (node.getParentNodeCode() != null && nodeCodes.contains(node.getParentNodeCode())) {
                adj.get(node.getParentNodeCode()).add(node.getNodeCode());
                inDegree.merge(node.getNodeCode(), 1, Integer::sum);
            }
        }
        for (DatasetNodeRelation rel : relations) {
            if (!rel.isEnabled()) continue;
            if (nodeCodes.contains(rel.getParentNodeCode()) && nodeCodes.contains(rel.getChildNodeCode())) {
                if (adj.get(rel.getParentNodeCode()).add(rel.getChildNodeCode())) {
                    inDegree.merge(rel.getChildNodeCode(), 1, Integer::sum);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (String code : nodeCodes) {
            if (inDegree.get(code) == 0) {
                queue.add(code);
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            result.add(cur);
            for (String next : adj.get(cur)) {
                int deg = inDegree.get(next) - 1;
                inDegree.put(next, deg);
                if (deg == 0) queue.add(next);
            }
        }

        if (result.size() < nodeCodes.size()) {
            resp.getErrors().add("Circular dependency detected among nodes");
        }
        return result;
    }

    private void checkDraft(DatasetDefinition def) {
        if (def.getStatus() != DatasetStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Can only modify datasets in DRAFT status");
        }
    }
}
