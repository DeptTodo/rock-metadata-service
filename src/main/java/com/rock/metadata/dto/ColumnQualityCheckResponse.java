package com.rock.metadata.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字段（或表）质量检查的汇总响应
 */
@Data
public class ColumnQualityCheckResponse {

    private Long datasourceId;
    private String schemaName;
    private String tableName;
    private String columnName;

    /** 总规则数 */
    private int totalRules;

    /** 通过数 */
    private int passedCount;

    /** 失败数 */
    private int failedCount;

    /** 总体通过率（百分比） */
    private double passRate;

    /** 每条规则的详细结果 */
    private List<QualityCheckResult> results;

    /** 执行时间 */
    private LocalDateTime executedAt;
}
