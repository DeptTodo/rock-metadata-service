package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 数据质量规则定义（可复用的规则模板）
 */
@Entity
@Table(name = "quality_rule", uniqueConstraints = {
    @UniqueConstraint(name = "uk_quality_rule_code", columnNames = "rule_code")
})
@Getter @Setter @NoArgsConstructor
public class QualityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 规则编码，唯一标识，如 NOT_NULL, RANGE_0_100 */
    @Column(name = "rule_code", nullable = false, length = 128)
    private String ruleCode;

    /** 规则名称 */
    @Column(name = "rule_name", nullable = false, length = 256)
    private String ruleName;

    /** 规则类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 32)
    private QualityRuleType ruleType;

    /** 规则描述 */
    @Column(name = "description", length = 1024)
    private String description;

    /** 默认严重级别 */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_severity", nullable = false, length = 16)
    private RuleSeverity defaultSeverity;

    /** 默认参数（JSON），如 {"min": 0, "max": 100} */
    @Column(name = "default_params", length = 2048)
    private String defaultParams;

    /** 是否内置规则（内置规则不可删除） */
    @Column(name = "is_built_in", nullable = false)
    private boolean builtIn;

    /** 是否启用 */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
