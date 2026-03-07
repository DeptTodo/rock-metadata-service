package com.rock.metadata.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单条质量规则的检查结果
 */
@Data
public class QualityCheckResult {

    private Long columnRuleId;
    private Long ruleId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private String severity;
    private String columnName;

    /** 是否通过 */
    private boolean passed;

    /** 总行数 */
    private long totalRows;

    /** 违规行数 */
    private long violationCount;

    /** 违规率（百分比） */
    private double violationRate;

    /** 违规样本值 */
    private List<String> sampleViolations;

    /** 结果消息 */
    private String message;

    /** 执行时间 */
    private LocalDateTime executedAt;
}
