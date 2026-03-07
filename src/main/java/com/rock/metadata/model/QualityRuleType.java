package com.rock.metadata.model;

/**
 * 数据质量规则类型
 */
public enum QualityRuleType {
    /** 非空检查 */
    NOT_NULL,
    /** 唯一性检查 */
    UNIQUE,
    /** 数值范围检查 (params: min, max) */
    VALUE_RANGE,
    /** 字符长度范围检查 (params: minLength, maxLength) */
    LENGTH_RANGE,
    /** 正则表达式匹配 (params: pattern) */
    REGEX_MATCH,
    /** 枚举值检查 (params: allowedValues) */
    ENUM_VALUES,
    /** 非空白检查（排除空字符串和纯空白） */
    NOT_BLANK,
    /** 自定义 SQL 表达式 (params: expression) */
    CUSTOM_SQL
}
