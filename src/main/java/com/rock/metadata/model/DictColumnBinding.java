package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 数据字典与字段的绑定关系 — 记录某个字典应用于哪些数据源/schema/表/列。
 * 支持跨数据源共享同一字典（如"性别"字典同时绑定到多个库的多个表）。
 */
@Entity
@Table(name = "dict_column_binding", indexes = {
    @Index(name = "idx_dict_binding_dict", columnList = "dict_id"),
    @Index(name = "idx_dict_binding_col", columnList = "meta_column_id"),
    @Index(name = "idx_dict_binding_ds", columnList = "datasource_id")
})
@Getter @Setter @NoArgsConstructor
public class DictColumnBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联的字典定义 */
    @Column(name = "dict_id", nullable = false)
    private Long dictId;

    /** 绑定目标：数据源 ID */
    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    /** 绑定目标：schema 名称 */
    @Column(name = "schema_name", length = 256)
    private String schemaName;

    /** 绑定目标：表名 */
    @Column(name = "table_name", nullable = false, length = 256)
    private String tableName;

    /** 绑定目标：列名 */
    @Column(name = "column_name", nullable = false, length = 256)
    private String columnName;

    /** 直接关联 MetaColumn（可选，crawl 后可自动填充） */
    @Column(name = "meta_column_id")
    private Long metaColumnId;

    /** 绑定方式: MANUAL(手工指定), NAME_MATCH(按命名规则匹配), LLM_INFERRED(LLM推断) */
    @Column(name = "binding_type", nullable = false, length = 32)
    private String bindingType;

    /** 绑定置信度（LLM 推断时使用，0.0 - 1.0） */
    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
