package com.rock.metadata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rock.metadata.dto.DatasetInstanceDetailResponse;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetExecutionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = 500;

    private final Set<String> activeExecutions = ConcurrentHashMap.newKeySet();

    @Value("${metadata.dataset.max-execution-time-seconds:300}")
    private int maxExecutionTimeSeconds;

    @Value("${metadata.dataset.max-rows-per-node:10000}")
    private int defaultMaxRowsPerNode;

    @Value("${metadata.dataset.query-timeout-seconds:30}")
    private int queryTimeoutSeconds;

    @Value("${metadata.dataset.max-snapshot-size-bytes:104857600}")
    private long maxSnapshotSizeBytes;

    @Value("${metadata.dataset.retain-instance-count:10}")
    private int retainInstanceCount;

    private final DatasetDefinitionRepository definitionRepository;
    private final DatasetNodeRepository nodeRepository;
    private final DatasetNodeRelationRepository relationRepository;
    private final DatasetNodeFilterRepository filterRepository;
    private final DatasetFieldMappingRepository fieldMappingRepository;
    private final DatasetTransformRuleRepository transformRuleRepository;
    private final DatasetInstanceRepository instanceRepository;
    private final DatasetInstanceSnapshotRepository snapshotRepository;
    private final DataSourceConfigRepository dataSourceConfigRepository;
    private final DictDefinitionRepository dictDefinitionRepository;
    private final DictItemRepository dictItemRepository;
    private final TargetDataSourceManager targetDataSourceManager;

    @Transactional
    public DatasetInstance executeDataset(Long datasetId, String rootKeyValue,
                                           Map<String, String> params) {
        DatasetDefinition def = definitionRepository.findById(datasetId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dataset not found: " + datasetId));
        if (def.getStatus() != DatasetStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Can only execute PUBLISHED datasets");
        }

        DataSourceConfig dsConfig = dataSourceConfigRepository.findById(def.getDatasourceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + def.getDatasourceId()));

        DatasetInstance instance = new DatasetInstance();
        instance.setDatasetId(datasetId);
        instance.setDatasourceId(def.getDatasourceId());
        instance.setDatasetVersion(def.getVersion());
        instance.setExecutionStatus(DatasetExecutionStatus.PENDING);
        instance.setRootKeyValue(rootKeyValue);
        if (params != null && !params.isEmpty()) {
            try {
                instance.setExecutionParams(MAPPER.writeValueAsString(params));
            } catch (Exception e) {
                log.warn("Failed to serialize execution params", e);
            }
        }
        instance = instanceRepository.save(instance);

        executeDatasetAsync(instance, def, dsConfig, rootKeyValue, params);
        return instance;
    }

    @Async
    @Transactional
    public void executeDatasetAsync(DatasetInstance instance, DatasetDefinition def,
                                     DataSourceConfig dsConfig, String rootKeyValue,
                                     Map<String, String> params) {
        String executionKey = def.getId() + ":" + (rootKeyValue != null ? rootKeyValue : "ALL");
        if (!activeExecutions.add(executionKey)) {
            instance.setExecutionStatus(DatasetExecutionStatus.FAILED);
            instance.setErrorMessage("Duplicate execution in progress for this dataset");
            instance.setFinishedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            return;
        }

        String dbType = dsConfig.getDbType().toLowerCase();

        try {
            instance.setExecutionStatus(DatasetExecutionStatus.RUNNING);
            instance.setStartedAt(LocalDateTime.now());
            instanceRepository.save(instance);

            List<DatasetNode> allNodes = nodeRepository.findByDatasetIdOrderByExecutionOrder(def.getId());
            List<DatasetNode> enabledNodes = allNodes.stream().filter(DatasetNode::isEnabled).toList();
            List<DatasetNodeRelation> relations = relationRepository.findByDatasetId(def.getId());
            List<DatasetNodeRelation> enabledRelations = relations.stream()
                    .filter(DatasetNodeRelation::isEnabled).toList();

            // Topological sort
            List<String> executionOrder = topologicalSort(enabledNodes, enabledRelations);

            instance.setTotalNodes(executionOrder.size());
            Map<String, String> nodeProgress = new LinkedHashMap<>();

            // Build node lookup
            Map<String, DatasetNode> nodeMap = new LinkedHashMap<>();
            for (DatasetNode n : enabledNodes) nodeMap.put(n.getNodeCode(), n);

            // Build relation lookup: childNodeCode -> relation
            Map<String, DatasetNodeRelation> childRelationMap = new HashMap<>();
            for (DatasetNodeRelation r : enabledRelations) {
                childRelationMap.put(r.getChildNodeCode(), r);
            }

            // Load field mappings per node
            Map<String, List<DatasetFieldMapping>> nodeMappings = new HashMap<>();
            List<DatasetFieldMapping> allMappings = fieldMappingRepository.findByDatasetId(def.getId());
            for (DatasetFieldMapping m : allMappings) {
                if (m.isEnabled()) {
                    nodeMappings.computeIfAbsent(m.getNodeCode(), k -> new ArrayList<>()).add(m);
                }
            }

            // Load filters per node
            Map<String, List<DatasetNodeFilter>> nodeFilters = new HashMap<>();
            List<DatasetNodeFilter> allFilters = filterRepository.findByDatasetId(def.getId());
            for (DatasetNodeFilter f : allFilters) {
                if (f.isEnabled()) {
                    nodeFilters.computeIfAbsent(f.getNodeCode(), k -> new ArrayList<>()).add(f);
                }
            }

            // Pre-load dict cache for DICT_LOOKUP transforms
            Map<String, List<DictItem>> dictCache = loadDictCache(allMappings);

            // Pre-load transform rules
            Map<Long, DatasetTransformRule> transformRules = new HashMap<>();
            for (DatasetFieldMapping m : allMappings) {
                if (m.getTransformRuleId() != null) {
                    transformRuleRepository.findById(m.getTransformRuleId())
                            .ifPresent(r -> transformRules.put(r.getId(), r));
                }
            }

            // Build required join columns per node for tree assembly
            // Each node needs its child join column (from relations where it's child)
            // and parent join columns (from relations where it's parent)
            Map<String, Set<String>> requiredJoinColumns = new HashMap<>();
            // Track frequency of parentJoinColumn per node to determine ROOT key column
            Map<String, Map<String, Integer>> parentJoinFrequency = new HashMap<>();
            for (DatasetNodeRelation r : enabledRelations) {
                if (r.getChildJoinColumn() != null) {
                    requiredJoinColumns.computeIfAbsent(r.getChildNodeCode(),
                            k -> new LinkedHashSet<>()).add(r.getChildJoinColumn());
                }
                if (r.getParentJoinColumn() != null) {
                    requiredJoinColumns.computeIfAbsent(r.getParentNodeCode(),
                            k -> new LinkedHashSet<>()).add(r.getParentJoinColumn());
                    parentJoinFrequency.computeIfAbsent(r.getParentNodeCode(),
                            k -> new HashMap<>())
                            .merge(r.getParentJoinColumn(), 1, Integer::sum);
                }
            }
            // Determine the ROOT node's key column: the most frequently used parentJoinColumn
            String rootKeyColumn = null;
            Map<String, Integer> rootFreq = parentJoinFrequency.get(def.getRootNodeCode());
            if (rootFreq != null) {
                rootKeyColumn = rootFreq.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey).orElse(null);
            }

            // Execute each node
            // nodeCode -> list of row maps
            Map<String, List<Map<String, Object>>> nodeResults = new LinkedHashMap<>();
            int successCount = 0;
            int failedCount = 0;
            long totalRows = 0;
            Set<String> failedNodes = new HashSet<>();

            try (Connection conn = targetDataSourceManager.getConnection(dsConfig)) {
                conn.setReadOnly(true);

                for (String nodeCode : executionOrder) {
                    DatasetNode node = nodeMap.get(nodeCode);
                    if (node == null) continue;

                    // Skip if any ancestor failed
                    if (isAncestorFailed(node, nodeMap, failedNodes)) {
                        nodeProgress.put(nodeCode, "SKIPPED");
                        failedCount++;
                        failedNodes.add(nodeCode);
                        continue;
                    }

                    try {
                        List<Map<String, Object>> rows = executeNode(
                                conn, node, dbType, def,
                                nodeMappings.get(nodeCode),
                                nodeFilters.get(nodeCode),
                                childRelationMap.get(nodeCode),
                                nodeResults, params, rootKeyValue, rootKeyColumn,
                                transformRules, dictCache, nodeMappings,
                                requiredJoinColumns.getOrDefault(nodeCode, Set.of()));

                        nodeResults.put(nodeCode, rows);
                        totalRows += rows.size();
                        successCount++;
                        nodeProgress.put(nodeCode, "SUCCESS");

                        // Save per-node snapshot
                        saveNodeSnapshot(instance.getId(), nodeCode, rows);

                    } catch (Exception e) {
                        log.error("Node '{}' execution failed: {}", nodeCode, e.getMessage(), e);
                        failedCount++;
                        failedNodes.add(nodeCode);
                        nodeProgress.put(nodeCode, "FAILED: " + truncate(e.getMessage(), 200));

                        // ROOT failure = entire instance fails
                        if ("ROOT".equals(node.getNodeType())) {
                            throw new RuntimeException("ROOT node failed: " + e.getMessage(), e);
                        }
                    }
                }
            }

            // Assemble final output
            String outputJson;
            if ("FLAT".equalsIgnoreCase(def.getOutputFormat())) {
                outputJson = MAPPER.writeValueAsString(nodeResults);
            } else {
                Object tree = assembleTree(def.getRootNodeCode(), nodeMap, nodeResults,
                        enabledRelations, nodeMappings);
                outputJson = MAPPER.writeValueAsString(tree);
            }

            // Save aggregate snapshot
            saveAggregateSnapshot(instance.getId(), outputJson);

            instance.setSuccessNodes(successCount);
            instance.setFailedNodes(failedCount);
            instance.setTotalRows(totalRows);
            instance.setNodeProgress(MAPPER.writeValueAsString(nodeProgress));

            if (failedCount == 0) {
                instance.setExecutionStatus(DatasetExecutionStatus.SUCCESS);
            } else {
                instance.setExecutionStatus(DatasetExecutionStatus.PARTIAL_SUCCESS);
            }

        } catch (Exception e) {
            log.error("Dataset execution {} failed", instance.getId(), e);
            instance.setExecutionStatus(DatasetExecutionStatus.FAILED);
            instance.setErrorMessage(truncate(e.getMessage(), 4000));
        } finally {
            instance.setFinishedAt(LocalDateTime.now());
            instanceRepository.save(instance);
            activeExecutions.remove(executionKey);
            cleanupOldInstances(def.getId());
        }
    }

    private List<Map<String, Object>> executeNode(
            Connection conn, DatasetNode node, String dbType, DatasetDefinition def,
            List<DatasetFieldMapping> mappings, List<DatasetNodeFilter> filters,
            DatasetNodeRelation relation,
            Map<String, List<Map<String, Object>>> parentResults,
            Map<String, String> params, String rootKeyValue, String rootKeyColumn,
            Map<Long, DatasetTransformRule> transformRules,
            Map<String, List<DictItem>> dictCache,
            Map<String, List<DatasetFieldMapping>> allNodeMappings,
            Set<String> requiredJoinColumns) throws SQLException {

        int maxRows = Math.min(node.getMaxRows() != null ? node.getMaxRows() : defaultMaxRowsPerNode,
                defaultMaxRowsPerNode);

        String qualifiedTable = JdbcUrlBuilder.qualifyTable(dbType, node.getSourceSchema(),
                node.getSourceTable());

        // Build SELECT columns
        StringBuilder selectCols = new StringBuilder();
        List<DatasetFieldMapping> sqlTransformMappings = new ArrayList<>();
        if (mappings != null && !mappings.isEmpty()) {
            for (int i = 0; i < mappings.size(); i++) {
                DatasetFieldMapping m = mappings.get(i);
                if (i > 0) selectCols.append(", ");

                if (m.getInlineExpression() != null && !m.getInlineExpression().isBlank()) {
                    if (DatasetTransformEngine.containsDangerousKeywords(m.getInlineExpression())) {
                        throw new SQLException("Dangerous SQL in inline expression for field: "
                                + m.getOutputField());
                    }
                    selectCols.append(m.getInlineExpression()).append(" AS ")
                            .append(JdbcUrlBuilder.quoteIdentifier(dbType, m.getOutputField()));
                } else if (m.getTransformRuleId() != null && transformRules.containsKey(m.getTransformRuleId())) {
                    DatasetTransformRule rule = transformRules.get(m.getTransformRuleId());
                    if ("SQL_EXPRESSION".equals(rule.getRuleType())
                            || "CONSTANT".equals(rule.getRuleType())
                            || "CONCATENATION".equals(rule.getRuleType())) {
                        String expr = DatasetTransformEngine.buildSqlExpression(
                                m.getSourceField(), rule.getRuleType(), rule.getRuleContent(), dbType);
                        selectCols.append(expr).append(" AS ")
                                .append(JdbcUrlBuilder.quoteIdentifier(dbType, m.getOutputField()));
                    } else {
                        // Post-query transform (DICT_LOOKUP, FORMAT)
                        selectCols.append(JdbcUrlBuilder.quoteIdentifier(dbType, m.getSourceField()))
                                .append(" AS ")
                                .append(JdbcUrlBuilder.quoteIdentifier(dbType, m.getOutputField()));
                        sqlTransformMappings.add(m);
                    }
                } else {
                    selectCols.append(JdbcUrlBuilder.quoteIdentifier(dbType, m.getSourceField()))
                            .append(" AS ")
                            .append(JdbcUrlBuilder.quoteIdentifier(dbType, m.getOutputField()));
                }
            }
        } else {
            selectCols.append("*");
        }

        // Ensure join columns are included in SELECT for tree assembly
        // Without these, parent-child matching in assembleTree would fail
        if (mappings != null && !mappings.isEmpty() && !requiredJoinColumns.isEmpty()) {
            Set<String> selectedSourceFields = new HashSet<>();
            for (DatasetFieldMapping m : mappings) {
                selectedSourceFields.add(m.getSourceField().toLowerCase());
            }
            for (String joinCol : requiredJoinColumns) {
                if (!selectedSourceFields.contains(joinCol.toLowerCase())) {
                    selectCols.append(", ").append(JdbcUrlBuilder.quoteIdentifier(dbType, joinCol));
                }
            }
        }

        // Build WHERE clauses
        List<String> whereClauses = new ArrayList<>();
        List<Object> whereParams = new ArrayList<>();

        // Parent key join (for non-root nodes)
        if (relation != null && node.getParentNodeCode() != null) {
            List<Map<String, Object>> parentRows = parentResults.get(relation.getParentNodeCode());
            if (parentRows == null || parentRows.isEmpty()) {
                return List.of(); // Parent has no rows, child should return empty too
            }
            String parentCol = relation.getParentJoinColumn();
            String childCol = relation.getChildJoinColumn();

            if ("CUSTOM_SQL".equals(relation.getRelationType())) {
                if (relation.getJoinExpression() != null) {
                    if (DatasetTransformEngine.containsDangerousKeywords(relation.getJoinExpression())) {
                        throw new SQLException("Dangerous SQL in join expression");
                    }
                    whereClauses.add("(" + relation.getJoinExpression() + ")");
                }
            } else if (parentCol != null && childCol != null) {
                // Resolve the parent join column to the output field name
                // Parent rows use output field names (from field mappings), not source column names
                String parentLookupKey = resolveOutputFieldName(
                        relation.getParentNodeCode(), parentCol, allNodeMappings);

                Set<Object> parentKeyValues = new LinkedHashSet<>();
                for (Map<String, Object> row : parentRows) {
                    Object val = row.get(parentLookupKey);
                    if (val == null) {
                        // Try case-insensitive lookup
                        for (Map.Entry<String, Object> e : row.entrySet()) {
                            if (e.getKey().equalsIgnoreCase(parentLookupKey)) {
                                val = e.getValue();
                                break;
                            }
                        }
                    }
                    if (val != null) parentKeyValues.add(val);
                }
                if (parentKeyValues.isEmpty()) {
                    return List.of(); // No parent keys, no child results
                }
                // Batch IN clause
                List<Object> keyList = new ArrayList<>(parentKeyValues);
                StringBuilder inClause = new StringBuilder();
                inClause.append(JdbcUrlBuilder.quoteIdentifier(dbType, childCol)).append(" IN (");
                int count = Math.min(keyList.size(), BATCH_SIZE);
                for (int i = 0; i < count; i++) {
                    if (i > 0) inClause.append(", ");
                    inClause.append("?");
                    whereParams.add(keyList.get(i));
                }
                inClause.append(")");
                whereClauses.add(inClause.toString());
            }
        }

        // Root key value filter for ROOT node
        if ("ROOT".equals(node.getNodeType()) && rootKeyValue != null && !rootKeyValue.isBlank()) {
            if (rootKeyColumn != null) {
                whereClauses.add(JdbcUrlBuilder.quoteIdentifier(dbType, rootKeyColumn) + " = ?");
                whereParams.add(rootKeyValue);
            }
        }

        // User-defined filters
        if (filters != null) {
            for (DatasetNodeFilter f : filters) {
                if (DatasetTransformEngine.containsDangerousKeywords(f.getFilterExpression())) {
                    throw new SQLException("Dangerous SQL in filter: " + f.getFilterName());
                }
                if (f.isParameterized() && f.getParamName() != null) {
                    String paramValue = params != null ? params.get(f.getParamName()) : null;
                    if (paramValue == null) paramValue = f.getDefaultValue();
                    if (paramValue != null) {
                        String expr = f.getFilterExpression().replace("${" + f.getParamName() + "}", "?");
                        whereClauses.add("(" + expr + ")");
                        whereParams.add(paramValue);
                    } else if (f.isRequired()) {
                        throw new SQLException("Required parameter missing: " + f.getParamName());
                    }
                } else {
                    whereClauses.add("(" + f.getFilterExpression() + ")");
                }
            }
        }

        // Build final SQL
        StringBuilder sql = new StringBuilder();
        sql.append(DatasetTransformEngine.buildSelectPrefix(dbType, maxRows));
        sql.append(selectCols);
        sql.append(" FROM ").append(qualifiedTable);
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }
        sql.append(DatasetTransformEngine.buildLimitClause(dbType, maxRows));

        log.debug("Executing node '{}': {}", node.getNodeCode(), sql);

        // Execute query
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setQueryTimeout(queryTimeoutSeconds);
            stmt.setMaxRows(maxRows);
            for (int i = 0; i < whereParams.size(); i++) {
                stmt.setObject(i + 1, whereParams.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                List<String> colNames = new ArrayList<>();
                for (int i = 1; i <= colCount; i++) {
                    colNames.add(meta.getColumnLabel(i));
                }

                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next() && rows.size() < maxRows) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 0; i < colCount; i++) {
                        Object val = rs.getObject(i + 1);
                        row.put(colNames.get(i), val);
                    }
                    rows.add(row);
                }

                // Apply post-query transforms
                if (!sqlTransformMappings.isEmpty()) {
                    for (Map<String, Object> row : rows) {
                        for (DatasetFieldMapping m : sqlTransformMappings) {
                            DatasetTransformRule rule = transformRules.get(m.getTransformRuleId());
                            if (rule != null) {
                                Object originalVal = row.get(m.getOutputField());
                                Object transformed = DatasetTransformEngine.applyTransform(
                                        originalVal, rule.getRuleType(), rule.getRuleContent(),
                                        dictCache);
                                row.put(m.getOutputField(), transformed);
                            }
                        }
                    }
                }

                // Apply defaults for missing required fields
                if (mappings != null) {
                    for (Map<String, Object> row : rows) {
                        for (DatasetFieldMapping m : mappings) {
                            if (m.getDefaultValue() != null && row.get(m.getOutputField()) == null) {
                                row.put(m.getOutputField(), m.getDefaultValue());
                            }
                        }
                    }
                }

                return rows;
            }
        }
    }

    /**
     * Resolves the output field name for a given source column in a node's field mappings.
     * Parent rows use output field names (aliases), so we need to map source column → output field.
     */
    private String resolveOutputFieldName(String nodeCode, String sourceColumn,
                                          Map<String, List<DatasetFieldMapping>> allNodeMappings) {
        List<DatasetFieldMapping> parentMappings = allNodeMappings.get(nodeCode);
        if (parentMappings != null) {
            for (DatasetFieldMapping m : parentMappings) {
                if (sourceColumn.equalsIgnoreCase(m.getSourceField())) {
                    return m.getOutputField();
                }
            }
        }
        // Fallback: if no mapping found, use the source column name as-is (SELECT * case)
        return sourceColumn;
    }

    private Object assembleTree(String rootNodeCode,
                                 Map<String, DatasetNode> nodeMap,
                                 Map<String, List<Map<String, Object>>> nodeResults,
                                 List<DatasetNodeRelation> relations,
                                 Map<String, List<DatasetFieldMapping>> allNodeMappings) {
        List<Map<String, Object>> rootRows = nodeResults.get(rootNodeCode);
        if (rootRows == null || rootRows.isEmpty()) return List.of();

        // Build parent->children mapping from relations
        Map<String, List<DatasetNodeRelation>> parentChildRelations = new HashMap<>();
        for (DatasetNodeRelation rel : relations) {
            parentChildRelations.computeIfAbsent(rel.getParentNodeCode(),
                    k -> new ArrayList<>()).add(rel);
        }

        // Also use parentNodeCode from DatasetNode
        for (DatasetNode node : nodeMap.values()) {
            if (node.getParentNodeCode() != null) {
                boolean hasRelation = parentChildRelations
                        .getOrDefault(node.getParentNodeCode(), List.of())
                        .stream()
                        .anyMatch(r -> r.getChildNodeCode().equals(node.getNodeCode()));
                if (!hasRelation) {
                    DatasetNodeRelation syntheticRel = new DatasetNodeRelation();
                    syntheticRel.setParentNodeCode(node.getParentNodeCode());
                    syntheticRel.setChildNodeCode(node.getNodeCode());
                    parentChildRelations.computeIfAbsent(node.getParentNodeCode(),
                            k -> new ArrayList<>()).add(syntheticRel);
                }
            }
        }

        // Nest children into each root row
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> rootRow : rootRows) {
            Map<String, Object> enriched = new LinkedHashMap<>(rootRow);
            nestChildren(enriched, rootNodeCode, nodeMap, nodeResults,
                    parentChildRelations, allNodeMappings);
            result.add(enriched);
        }
        return result;
    }

    private void nestChildren(Map<String, Object> parentRow, String parentNodeCode,
                               Map<String, DatasetNode> nodeMap,
                               Map<String, List<Map<String, Object>>> nodeResults,
                               Map<String, List<DatasetNodeRelation>> parentChildRelations,
                               Map<String, List<DatasetFieldMapping>> allNodeMappings) {
        List<DatasetNodeRelation> childRels = parentChildRelations.get(parentNodeCode);
        if (childRels == null) return;

        for (DatasetNodeRelation rel : childRels) {
            String childCode = rel.getChildNodeCode();
            DatasetNode childNode = nodeMap.get(childCode);
            if (childNode == null) continue;

            List<Map<String, Object>> childRows = nodeResults.get(childCode);
            if (childRows == null) continue;

            // Filter child rows matching parent key
            List<Map<String, Object>> matchedRows;
            if (rel.getParentJoinColumn() != null && rel.getChildJoinColumn() != null) {
                // Resolve join columns to output field names
                String parentLookupKey = resolveOutputFieldName(
                        rel.getParentNodeCode(), rel.getParentJoinColumn(), allNodeMappings);
                String childLookupKey = resolveOutputFieldName(
                        rel.getChildNodeCode(), rel.getChildJoinColumn(), allNodeMappings);

                Object parentKeyVal = getValueCaseInsensitive(parentRow, parentLookupKey);
                matchedRows = new ArrayList<>();
                for (Map<String, Object> childRow : childRows) {
                    Object childKeyVal = getValueCaseInsensitive(childRow, childLookupKey);
                    if (parentKeyVal != null && parentKeyVal.toString().equals(
                            childKeyVal != null ? childKeyVal.toString() : "")) {
                        matchedRows.add(childRow);
                    }
                }
            } else {
                matchedRows = childRows;
            }

            // Recursively nest
            List<Map<String, Object>> nestedRows = new ArrayList<>();
            for (Map<String, Object> childRow : matchedRows) {
                Map<String, Object> enriched = new LinkedHashMap<>(childRow);
                nestChildren(enriched, childCode, nodeMap, nodeResults, parentChildRelations,
                        allNodeMappings);
                nestedRows.add(enriched);
            }

            String cardinality = childNode.getCardinality();
            if ("ONE_TO_ONE".equals(cardinality) || "CHILD_OBJECT".equals(childNode.getNodeType())) {
                parentRow.put(childCode, nestedRows.isEmpty() ? null : nestedRows.get(0));
            } else {
                parentRow.put(childCode, nestedRows);
            }
        }
    }

    private Object getValueCaseInsensitive(Map<String, Object> row, String key) {
        Object val = row.get(key);
        if (val != null) return val;
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        return null;
    }

    private boolean isAncestorFailed(DatasetNode node, Map<String, DatasetNode> nodeMap,
                                      Set<String> failedNodes) {
        String parentCode = node.getParentNodeCode();
        while (parentCode != null) {
            if (failedNodes.contains(parentCode)) return true;
            DatasetNode parent = nodeMap.get(parentCode);
            if (parent == null) break;
            parentCode = parent.getParentNodeCode();
        }
        return false;
    }

    private List<String> topologicalSort(List<DatasetNode> nodes,
                                          List<DatasetNodeRelation> relations) {
        Set<String> codes = new LinkedHashSet<>();
        for (DatasetNode n : nodes) codes.add(n.getNodeCode());

        Map<String, Set<String>> adj = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (String c : codes) {
            adj.put(c, new HashSet<>());
            inDegree.put(c, 0);
        }

        for (DatasetNode node : nodes) {
            if (node.getParentNodeCode() != null && codes.contains(node.getParentNodeCode())) {
                adj.get(node.getParentNodeCode()).add(node.getNodeCode());
                inDegree.merge(node.getNodeCode(), 1, Integer::sum);
            }
        }
        for (DatasetNodeRelation rel : relations) {
            if (codes.contains(rel.getParentNodeCode()) && codes.contains(rel.getChildNodeCode())) {
                if (adj.get(rel.getParentNodeCode()).add(rel.getChildNodeCode())) {
                    inDegree.merge(rel.getChildNodeCode(), 1, Integer::sum);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (String c : codes) {
            if (inDegree.get(c) == 0) queue.add(c);
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
        return result;
    }

    private Map<String, List<DictItem>> loadDictCache(List<DatasetFieldMapping> mappings) {
        Map<String, List<DictItem>> cache = new HashMap<>();
        Set<Long> ruleIds = new HashSet<>();
        for (DatasetFieldMapping m : mappings) {
            if (m.getTransformRuleId() != null) ruleIds.add(m.getTransformRuleId());
        }
        for (Long ruleId : ruleIds) {
            transformRuleRepository.findById(ruleId).ifPresent(rule -> {
                if ("DICT_LOOKUP".equals(rule.getRuleType())) {
                    try {
                        com.fasterxml.jackson.databind.JsonNode config =
                                MAPPER.readTree(rule.getRuleContent());
                        String dictCode = config.path("dictCode").asText();
                        if (!dictCode.isBlank() && !cache.containsKey(dictCode)) {
                            cache.put(dictCode, dictItemRepository.findByDictIdOrderBySortOrder(
                                    findDictIdByCode(dictCode)));
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load dict cache for rule {}: {}", ruleId, e.getMessage());
                    }
                }
            });
        }
        return cache;
    }

    private Long findDictIdByCode(String dictCode) {
        return dictDefinitionRepository.findByDictCode(dictCode)
                .map(DictDefinition::getId)
                .orElse(0L);
    }

    private void saveNodeSnapshot(Long instanceId, String nodeCode,
                                   List<Map<String, Object>> rows) {
        try {
            String json = MAPPER.writeValueAsString(rows);
            if (json.length() > maxSnapshotSizeBytes) {
                log.warn("Node '{}' snapshot exceeds max size, truncating", nodeCode);
                json = json.substring(0, (int) maxSnapshotSizeBytes);
            }
            DatasetInstanceSnapshot snapshot = new DatasetInstanceSnapshot();
            snapshot.setInstanceId(instanceId);
            snapshot.setNodeCode(nodeCode);
            snapshot.setSnapshotJson(json);
            snapshot.setSnapshotHash(sha256(json));
            snapshot.setSizeBytes((long) json.getBytes(StandardCharsets.UTF_8).length);
            snapshot.setRowCount(rows.size());
            snapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.error("Failed to save snapshot for node '{}'", nodeCode, e);
        }
    }

    private void saveAggregateSnapshot(Long instanceId, String json) {
        try {
            if (json.length() > maxSnapshotSizeBytes) {
                log.warn("Aggregate snapshot exceeds max size, truncating");
                json = json.substring(0, (int) maxSnapshotSizeBytes);
            }
            DatasetInstanceSnapshot snapshot = new DatasetInstanceSnapshot();
            snapshot.setInstanceId(instanceId);
            snapshot.setNodeCode(null); // null = aggregate
            snapshot.setSnapshotJson(json);
            snapshot.setSnapshotHash(sha256(json));
            snapshot.setSizeBytes((long) json.getBytes(StandardCharsets.UTF_8).length);
            snapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.error("Failed to save aggregate snapshot", e);
        }
    }

    private void cleanupOldInstances(Long datasetId) {
        try {
            List<DatasetInstance> instances = instanceRepository
                    .findByDatasetIdOrderByCreatedAtDesc(datasetId);
            if (instances.size() > retainInstanceCount) {
                List<DatasetInstance> toDelete = instances.subList(retainInstanceCount, instances.size());
                for (DatasetInstance inst : toDelete) {
                    snapshotRepository.deleteByInstanceId(inst.getId());
                }
                instanceRepository.deleteAll(toDelete);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup old instances for dataset {}", datasetId, e);
        }
    }

    // ===== Query Methods =====

    @Transactional(readOnly = true)
    public DatasetInstance getInstance(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Instance not found: " + id));
    }

    @Transactional(readOnly = true)
    public DatasetInstanceDetailResponse getInstanceDetail(Long id) {
        DatasetInstance instance = getInstance(id);
        DatasetInstanceDetailResponse resp = new DatasetInstanceDetailResponse();
        resp.setInstance(instance);
        resp.setSnapshots(snapshotRepository.findByInstanceId(id));
        return resp;
    }

    @Transactional(readOnly = true)
    public List<DatasetInstance> listInstances(Long datasetId, String status) {
        if (status != null) {
            return instanceRepository.findByDatasetIdAndExecutionStatusOrderByCreatedAtDesc(
                    datasetId, DatasetExecutionStatus.valueOf(status));
        }
        return instanceRepository.findByDatasetIdOrderByCreatedAtDesc(datasetId);
    }

    @Transactional(readOnly = true)
    public DatasetInstanceSnapshot getSnapshot(Long instanceId, String nodeCode) {
        if (nodeCode == null) {
            return snapshotRepository.findByInstanceIdAndNodeCodeIsNull(instanceId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Aggregate snapshot not found"));
        }
        return snapshotRepository.findByInstanceIdAndNodeCode(instanceId, nodeCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Snapshot not found for node: " + nodeCode));
    }

    @Transactional
    public void deleteInstance(Long id) {
        if (!instanceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Instance not found: " + id);
        }
        snapshotRepository.deleteByInstanceId(id);
        instanceRepository.deleteById(id);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
