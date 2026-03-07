package com.rock.metadata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rock.metadata.model.DictItem;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public final class DatasetTransformEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DatasetTransformEngine() {}

    public static String buildSqlExpression(String sourceCol, String ruleType,
                                             String ruleContent, String dbType) {
        try {
            JsonNode config = MAPPER.readTree(ruleContent);
            return switch (ruleType) {
                case "SQL_EXPRESSION" -> {
                    String expr = config.path("expression").asText("");
                    yield expr.replace("${sourceField}",
                            JdbcUrlBuilder.quoteIdentifier(dbType, sourceCol));
                }
                case "CONSTANT" -> "'" + config.path("value").asText("").replace("'", "''") + "'";
                case "CONCATENATION" -> {
                    StringBuilder sb = new StringBuilder("CONCAT(");
                    JsonNode fields = config.path("fields");
                    if (fields.isArray()) {
                        for (int i = 0; i < fields.size(); i++) {
                            if (i > 0) sb.append(", ");
                            String f = fields.get(i).asText();
                            if (f.startsWith("'")) {
                                sb.append(f);
                            } else {
                                sb.append(JdbcUrlBuilder.quoteIdentifier(dbType, f));
                            }
                        }
                    }
                    sb.append(")");
                    yield sb.toString();
                }
                default -> JdbcUrlBuilder.quoteIdentifier(dbType, sourceCol);
            };
        } catch (Exception e) {
            log.warn("Failed to build SQL expression for column {}: {}", sourceCol, e.getMessage());
            return JdbcUrlBuilder.quoteIdentifier(dbType, sourceCol);
        }
    }

    public static Object applyTransform(Object value, String ruleType, String ruleContent,
                                          Map<String, List<DictItem>> dictCache) {
        if (value == null) return null;
        try {
            JsonNode config = MAPPER.readTree(ruleContent);
            return switch (ruleType) {
                case "DICT_LOOKUP" -> {
                    String dictCode = config.path("dictCode").asText();
                    String fallback = config.path("fallback").asText("original");
                    yield applyDictLookup(value, dictCode, fallback, dictCache);
                }
                case "FORMAT" -> {
                    String pattern = config.path("pattern").asText();
                    yield applyFormat(value, pattern);
                }
                case "CONSTANT" -> config.path("value").asText("");
                default -> value;
            };
        } catch (Exception e) {
            log.warn("Transform failed for value '{}' with rule type '{}': {}",
                    value, ruleType, e.getMessage());
            return value;
        }
    }

    private static Object applyDictLookup(Object value, String dictCode, String fallback,
                                            Map<String, List<DictItem>> dictCache) {
        List<DictItem> items = dictCache.get(dictCode);
        if (items == null) return value;
        String strVal = String.valueOf(value);
        for (DictItem item : items) {
            if (strVal.equals(item.getItemCode())) {
                return item.getItemValue();
            }
        }
        return "original".equals(fallback) ? value : fallback;
    }

    private static Object applyFormat(Object value, String pattern) {
        if (value instanceof Date d) {
            return new SimpleDateFormat(pattern).format(d);
        }
        if (value instanceof java.sql.Timestamp ts) {
            return new SimpleDateFormat(pattern).format(ts);
        }
        if (value instanceof java.sql.Date d) {
            return new SimpleDateFormat(pattern).format(d);
        }
        return value;
    }

    public static String buildLimitClause(String dbType, int maxRows) {
        return switch (dbType.toLowerCase()) {
            case "oracle" -> " FETCH FIRST " + maxRows + " ROWS ONLY";
            case "sqlserver" -> ""; // handled via TOP in SELECT
            default -> " LIMIT " + maxRows;
        };
    }

    public static String buildSelectPrefix(String dbType, int maxRows) {
        if ("sqlserver".equalsIgnoreCase(dbType)) {
            return "SELECT TOP " + maxRows + " ";
        }
        return "SELECT ";
    }

    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "DROP ", "DELETE ", "INSERT ", "UPDATE ", "ALTER ", "CREATE ",
            "GRANT ", "REVOKE ", "TRUNCATE ", "EXEC ", "EXECUTE ",
            "INTO ", "MERGE ", "CALL "
    );

    public static boolean containsDangerousKeywords(String expression) {
        if (expression == null) return false;
        String upper = expression.toUpperCase().trim();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upper.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
