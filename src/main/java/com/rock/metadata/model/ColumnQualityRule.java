package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 字段质量规则绑定 — 将质量规则应用到具体的数据源字段上，支持参数覆盖。
 */
@Entity
@Table(name = "column_quality_rule", indexes = {
    @Index(name = "idx_cqr_rule", columnList = "rule_id"),
    @Index(name = "idx_cqr_column", columnList = "meta_column_id"),
    @Index(name = "idx_cqr_datasource", columnList = "datasource_id")
})
@Getter @Setter @NoArgsConstructor
public class ColumnQualityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的规则定义 */
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    /** 数据源 ID */
    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    /** schema 名称 */
    @Column(name = "schema_name", length = 256)
    private String schemaName;

    /** 表名 */
    @Column(name = "table_name", nullable = false, length = 256)
    private String tableName;

    /** 列名 */
    @Column(name = "column_name", nullable = false, length = 256)
    private String columnName;

    /** 关联 MetaColumn（可选，crawl 后可填充） */
    @Column(name = "meta_column_id")
    private Long metaColumnId;

    /** 覆盖的严重级别（为空则使用规则默认值） */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 16)
    private RuleSeverity severity;

    /** 覆盖的参数（JSON，为空则使用规则默认参数） */
    @Column(name = "params", length = 2048)
    private String params;

    /** 是否启用 */
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

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
