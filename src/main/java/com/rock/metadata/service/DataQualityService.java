package com.rock.metadata.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rock.metadata.dto.ColumnQualityCheckResponse;
import com.rock.metadata.dto.QualityCheckResult;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityService {

    private static final int QUERY_TIMEOUT_SECONDS = 30;
    private static final int SAMPLE_VIOLATION_LIMIT = 5;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final QualityRuleRepository qualityRuleRepository;
    private final ColumnQualityRuleRepository columnQualityRuleRepository;
    private final DataSourceConfigRepository dataSourceConfigRepository;

    // ===== QualityRule CRUD =====

    @Transactional
    public QualityRule createRule(String ruleCode, String ruleName, String ruleType,
                                  String description, String defaultSeverity,
                                  String defaultParams) {
        if (qualityRuleRepository.existsByRuleCode(ruleCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rule code already exists: " + ruleCode);
        }
        QualityRule rule = new QualityRule();
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleName);
        rule.setRuleType(QualityRuleType.valueOf(ruleType));
        rule.setDescription(description);
        rule.setDefaultSeverity(RuleSeverity.valueOf(defaultSeverity));
        rule.setDefaultParams(defaultParams);
        rule.setBuiltIn(false);
        rule.setActive(true);
        return qualityRuleRepository.save(rule);
    }

    @Transactional(readOnly = true)
    public List<QualityRule> listRules(String ruleType, Boolean activeOnly) {
        if (ruleType != null) {
            return qualityRuleRepository.findByRuleType(QualityRuleType.valueOf(ruleType));
        }
        if (Boolean.TRUE.equals(activeOnly)) {
            return qualityRuleRepository.findByActiveTrue();
        }
        return qualityRuleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public QualityRule getRule(Long ruleId) {
        return qualityRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Rule not found: " + ruleId));
    }

    @Transactional
    public QualityRule updateRule(Long ruleId, String ruleName, String description,
                                  String defaultSeverity, String defaultParams, Boolean active) {
        QualityRule rule = getRule(ruleId);
        if (ruleName != null) rule.setRuleName(ruleName);
        if (description != null) rule.setDescription(description);
        if (defaultSeverity != null) rule.setDefaultSeverity(RuleSeverity.valueOf(defaultSeverity));
        if (defaultParams != null) rule.setDefaultParams(defaultParams);
        if (active != null) rule.setActive(active);
        return qualityRuleRepository.save(rule);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        QualityRule rule = getRule(ruleId);
        if (rule.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete built-in rule: " + rule.getRuleCode());
        }
        columnQualityRuleRepository.deleteByRuleId(ruleId);
        qualityRuleRepository.deleteById(ruleId);
    }

    // ===== ColumnQualityRule CRUD =====

    @Transactional
    public ColumnQualityRule bindRuleToColumn(Long ruleId, Long datasourceId, String schemaName,
                                               String tableName, String columnName,
                                               Long metaColumnId, String severity, String params) {
        getRule(ruleId); // validate rule exists
        ColumnQualityRule binding = new ColumnQualityRule();
        binding.setRuleId(ruleId);
        binding.setDatasourceId(datasourceId);
        binding.setSchemaName(schemaName);
        binding.setTableName(tableName);
        binding.setColumnName(columnName);
        binding.setMetaColumnId(metaColumnId);
        if (severity != null) binding.setSeverity(RuleSeverity.valueOf(severity));
        binding.setParams(params);
        binding.setEnabled(true);
        return columnQualityRuleRepository.save(binding);
    }

    @Transactional(readOnly = true)
    public List<ColumnQualityRule> listColumnRules(Long datasourceId, String schemaName,
                                                     String tableName, String columnName) {
        if (columnName != null) {
            return columnQualityRuleRepository
                    .findByDatasourceIdAndSchemaNameAndTableNameAndColumnName(
                            datasourceId, schemaName, tableName, columnName);
        }
        if (tableName != null) {
            return columnQualityRuleRepository
                    .findByDatasourceIdAndSchemaNameAndTableName(datasourceId, schemaName, tableName);
        }
        return columnQualityRuleRepository.findByDatasourceId(datasourceId);
    }

    @Transactional(readOnly = true)
    public List<ColumnQualityRule> listColumnRulesByMetaColumn(Long metaColumnId) {
        return columnQualityRuleRepository.findByMetaColumnId(metaColumnId);
    }

    @Transactional
    public ColumnQualityRule updateColumnRule(Long id, String severity, String params, Boolean enabled) {
        ColumnQualityRule binding = columnQualityRuleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Column quality rule not found: " + id));
        if (severity != null) binding.setSeverity(RuleSeverity.valueOf(severity));
        if (params != null) binding.setParams(params);
        if (enabled != null) binding.setEnabled(enabled);
        return columnQualityRuleRepository.save(binding);
    }

    @Transactional
    public void deleteColumnRule(Long id) {
        if (!columnQualityRuleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Column quality rule not found: " + id);
        }
        columnQualityRuleRepository.deleteById(id);
    }

    // ===== Quality Check Execution =====

    /**
     * 执行某个字段上所有已启用规则的质量检查
     */
    public ColumnQualityCheckResponse executeColumnCheck(Long datasourceId, String schemaName,
                                                          String tableName, String columnName) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));

        List<ColumnQualityRule> bindings = columnQualityRuleRepository
                .findByDatasourceIdAndSchemaNameAndTableNameAndColumnName(
                        datasourceId, schemaName, tableName, columnName);
        bindings = bindings.stream().filter(ColumnQualityRule::isEnabled).toList();

        if (bindings.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No enabled quality rules found for column: " +
                    schemaName + "." + tableName + "." + columnName);
        }

        String dbType = ds.getDbType().toLowerCase();
        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        String qualifiedTable = JdbcUrlBuilder.qualifyTable(dbType, schemaName, tableName);
        String quotedCol = JdbcUrlBuilder.quoteIdentifier(dbType, columnName);

        List<QualityCheckResult> results = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), ds.getPassword())) {
            long totalRows = getTotalRows(conn, qualifiedTable);

            for (ColumnQualityRule binding : bindings) {
                QualityRule rule = qualityRuleRepository.findById(binding.getRuleId()).orElse(null);
                if (rule == null) continue;

                QualityCheckResult result = executeRule(conn, dbType, qualifiedTable, quotedCol,
                        columnName, totalRows, rule, binding);
                result.setExecutedAt(now);
                results.add(result);
            }
        } catch (SQLException e) {
            log.error("Quality check failed for {}.{}.{}: {}", schemaName, tableName, columnName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quality check failed: " + e.getMessage());
        }

        ColumnQualityCheckResponse response = new ColumnQualityCheckResponse();
        response.setDatasourceId(datasourceId);
        response.setSchemaName(schemaName);
        response.setTableName(tableName);
        response.setColumnName(columnName);
        response.setTotalRules(results.size());
        response.setPassedCount((int) results.stream().filter(QualityCheckResult::isPassed).count());
        response.setFailedCount(response.getTotalRules() - response.getPassedCount());
        response.setPassRate(results.isEmpty() ? 100.0
                : (double) response.getPassedCount() / response.getTotalRules() * 100);
        response.setResults(results);
        response.setExecutedAt(now);
        return response;
    }

    /**
     * 执行某张表上所有已启用规则的质量检查
     */
    public List<ColumnQualityCheckResponse> executeTableCheck(Long datasourceId,
                                                                String schemaName, String tableName) {
        DataSourceConfig ds = dataSourceConfigRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found: " + datasourceId));

        List<ColumnQualityRule> bindings = columnQualityRuleRepository
                .findByDatasourceIdAndSchemaNameAndTableName(datasourceId, schemaName, tableName);
        bindings = bindings.stream().filter(ColumnQualityRule::isEnabled).toList();

        if (bindings.isEmpty()) {
            return List.of();
        }

        // Group by column
        Map<String, List<ColumnQualityRule>> byColumn = new LinkedHashMap<>();
        for (ColumnQualityRule b : bindings) {
            byColumn.computeIfAbsent(b.getColumnName(), k -> new ArrayList<>()).add(b);
        }

        String dbType = ds.getDbType().toLowerCase();
        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        String qualifiedTable = JdbcUrlBuilder.qualifyTable(dbType, schemaName, tableName);
        LocalDateTime now = LocalDateTime.now();

        List<ColumnQualityCheckResponse> responses = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, ds.getUsername(), ds.getPassword())) {
            long totalRows = getTotalRows(conn, qualifiedTable);

            for (var entry : byColumn.entrySet()) {
                String colName = entry.getKey();
                String quotedCol = JdbcUrlBuilder.quoteIdentifier(dbType, colName);
                List<QualityCheckResult> results = new ArrayList<>();

                for (ColumnQualityRule binding : entry.getValue()) {
                    QualityRule rule = qualityRuleRepository.findById(binding.getRuleId()).orElse(null);
                    if (rule == null) continue;
                    QualityCheckResult result = executeRule(conn, dbType, qualifiedTable, quotedCol,
                            colName, totalRows, rule, binding);
                    result.setExecutedAt(now);
                    results.add(result);
                }

                ColumnQualityCheckResponse colResp = new ColumnQualityCheckResponse();
                colResp.setDatasourceId(datasourceId);
                colResp.setSchemaName(schemaName);
                colResp.setTableName(tableName);
                colResp.setColumnName(colName);
                colResp.setTotalRules(results.size());
                colResp.setPassedCount((int) results.stream().filter(QualityCheckResult::isPassed).count());
                colResp.setFailedCount(colResp.getTotalRules() - colResp.getPassedCount());
                colResp.setPassRate(results.isEmpty() ? 100.0
                        : (double) colResp.getPassedCount() / colResp.getTotalRules() * 100);
                colResp.setResults(results);
                colResp.setExecutedAt(now);
                responses.add(colResp);
            }
        } catch (SQLException e) {
            log.error("Quality check failed for {}.{}: {}", schemaName, tableName, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Quality check failed: " + e.getMessage());
        }

        return responses;
    }

    // ===== Internal =====

    private QualityCheckResult executeRule(Connection conn, String dbType, String qualifiedTable,
                                            String quotedCol, String columnName, long totalRows,
                                            QualityRule rule, ColumnQualityRule binding) {
        QualityCheckResult result = new QualityCheckResult();
        result.setColumnRuleId(binding.getId());
        result.setRuleId(rule.getId());
        result.setRuleCode(rule.getRuleCode());
        result.setRuleName(rule.getRuleName());
        result.setRuleType(rule.getRuleType().name());
        result.setSeverity((binding.getSeverity() != null ? binding.getSeverity() : rule.getDefaultSeverity()).name());
        result.setColumnName(columnName);
        result.setTotalRows(totalRows);

        Map<String, String> params = mergeParams(rule.getDefaultParams(), binding.getParams());

        try {
            String violationWhere = buildViolationWhere(dbType, quotedCol, rule.getRuleType(), params);

            // Count violations
            long violationCount = countViolations(conn, qualifiedTable, violationWhere);
            result.setViolationCount(violationCount);
            result.setViolationRate(totalRows > 0 ? (double) violationCount / totalRows * 100 : 0);
            result.setPassed(violationCount == 0);
            result.setMessage(violationCount == 0
                    ? "All rows passed " + rule.getRuleCode()
                    : violationCount + " of " + totalRows + " rows violated " + rule.getRuleCode());

            // Sample violations
            if (violationCount > 0) {
                result.setSampleViolations(sampleViolations(conn, dbType, qualifiedTable,
                        quotedCol, violationWhere));
            } else {
                result.setSampleViolations(List.of());
            }
        } catch (SQLException e) {
            result.setPassed(false);
            result.setMessage("Check execution failed: " + e.getMessage());
            result.setSampleViolations(List.of());
            log.warn("Rule {} check failed on {}: {}", rule.getRuleCode(), columnName, e.getMessage());
        }

        return result;
    }

    /**
     * 根据规则类型构建违规行的 WHERE 条件
     */
    private String buildViolationWhere(String dbType, String quotedCol,
                                        QualityRuleType ruleType, Map<String, String> params) {
        return switch (ruleType) {
            case NOT_NULL -> quotedCol + " IS NULL";

            case UNIQUE -> quotedCol + " IN (SELECT " + quotedCol +
                    " FROM (SELECT " + quotedCol + ", COUNT(*) AS cnt" +
                    " FROM %TABLE% WHERE " + quotedCol + " IS NOT NULL" +
                    " GROUP BY " + quotedCol + " HAVING COUNT(*) > 1) dup_sub)";

            case VALUE_RANGE -> {
                String min = params.getOrDefault("min", null);
                String max = params.getOrDefault("max", null);
                List<String> conds = new ArrayList<>();
                conds.add(quotedCol + " IS NOT NULL");
                if (min != null) conds.add(quotedCol + " < " + min);
                if (max != null) conds.add(quotedCol + " > " + max);
                if (conds.size() == 1) {
                    yield "1=0"; // no range specified, nothing violates
                }
                yield conds.get(0) + " AND (" +
                        String.join(" OR ", conds.subList(1, conds.size())) + ")";
            }

            case LENGTH_RANGE -> {
                String minLen = params.getOrDefault("minLength", null);
                String maxLen = params.getOrDefault("maxLength", null);
                String lenFunc = dbType.equals("sqlserver") ? "LEN" : "LENGTH";
                List<String> conds = new ArrayList<>();
                conds.add(quotedCol + " IS NOT NULL");
                if (minLen != null) conds.add(lenFunc + "(" + quotedCol + ") < " + minLen);
                if (maxLen != null) conds.add(lenFunc + "(" + quotedCol + ") > " + maxLen);
                if (conds.size() == 1) {
                    yield "1=0";
                }
                yield conds.get(0) + " AND (" +
                        String.join(" OR ", conds.subList(1, conds.size())) + ")";
            }

            case REGEX_MATCH -> {
                String pattern = params.get("pattern");
                if (pattern == null) yield "1=0";
                // 查找不匹配正则的行
                String escapedPattern = pattern.replace("'", "''");
                yield switch (dbType) {
                    case "postgresql" -> quotedCol + " IS NOT NULL AND " +
                            quotedCol + " !~ '" + escapedPattern + "'";
                    case "mysql" -> quotedCol + " IS NOT NULL AND " +
                            quotedCol + " NOT REGEXP '" + escapedPattern + "'";
                    case "oracle" -> quotedCol + " IS NOT NULL AND " +
                            "NOT REGEXP_LIKE(" + quotedCol + ", '" + escapedPattern + "')";
                    case "sqlserver" -> quotedCol + " IS NOT NULL AND " +
                            quotedCol + " NOT LIKE '" + escapedPattern + "'";
                    default -> quotedCol + " IS NOT NULL AND " +
                            quotedCol + " !~ '" + escapedPattern + "'";
                };
            }

            case ENUM_VALUES -> {
                String allowed = params.get("allowedValues");
                if (allowed == null) yield "1=0";
                String[] values = allowed.split(",");
                StringJoiner sj = new StringJoiner(",");
                for (String v : values) {
                    sj.add("'" + v.trim().replace("'", "''") + "'");
                }
                yield quotedCol + " IS NOT NULL AND " + quotedCol + " NOT IN (" + sj + ")";
            }

            case NOT_BLANK -> quotedCol + " IS NULL OR TRIM(CAST(" + quotedCol + " AS VARCHAR(4000))) = ''";

            case CUSTOM_SQL -> {
                String expression = params.get("expression");
                yield expression != null ? expression : "1=0";
            }
        };
    }

    private long countViolations(Connection conn, String qualifiedTable, String violationWhere) throws SQLException {
        String where = violationWhere.replace("%TABLE%", qualifiedTable);
        String sql = "SELECT COUNT(*) FROM " + qualifiedTable + " WHERE " + where;
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private List<String> sampleViolations(Connection conn, String dbType, String qualifiedTable,
                                            String quotedCol, String violationWhere) throws SQLException {
        String where = violationWhere.replace("%TABLE%", qualifiedTable);
        String baseSql = "SELECT " + quotedCol + " FROM " + qualifiedTable + " WHERE " + where;
        String sql = buildLimitSql(dbType, baseSql, SAMPLE_VIOLATION_LIMIT);

        List<String> samples = new ArrayList<>();
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Object val = rs.getObject(1);
                    samples.add(val != null ? val.toString() : "<NULL>");
                }
            }
        }
        return samples;
    }

    private long getTotalRows(Connection conn, String qualifiedTable) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + qualifiedTable;
        try (Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    private Map<String, String> mergeParams(String defaultParamsJson, String overrideParamsJson) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (defaultParamsJson != null && !defaultParamsJson.isBlank()) {
            try {
                merged.putAll(MAPPER.readValue(defaultParamsJson, new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("Failed to parse default params: {}", defaultParamsJson);
            }
        }
        if (overrideParamsJson != null && !overrideParamsJson.isBlank()) {
            try {
                merged.putAll(MAPPER.readValue(overrideParamsJson, new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("Failed to parse override params: {}", overrideParamsJson);
            }
        }
        return merged;
    }

    private String buildLimitSql(String dbType, String baseSql, int limit) {
        return switch (dbType) {
            case "sqlserver" -> baseSql.replaceFirst("(?i)^SELECT ", "SELECT TOP " + limit + " ");
            case "oracle" -> "SELECT * FROM (" + baseSql + ") WHERE ROWNUM <= " + limit;
            default -> baseSql + " LIMIT " + limit;
        };
    }
}
