package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 数据字典定义 — 代表一个字典（如"性别"、"订单状态"、"行业分类"）。
 * 支持追溯来源数据源、schema、表，便于迁移和溯源。
 */
@Entity
@Table(name = "dict_definition", indexes = {
    @Index(name = "idx_dict_def_ds", columnList = "datasource_id"),
    @Index(name = "idx_dict_def_code", columnList = "dict_code")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_dict_def_code", columnNames = {"dict_code"})
})
@Getter @Setter @NoArgsConstructor
public class DictDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===== 字典基本信息 =====

    /** 字典编码，全局唯一，如 "GENDER", "ORDER_STATUS", "INDUSTRY_CODE" */
    @Column(name = "dict_code", nullable = false, length = 128)
    private String dictCode;

    /** 字典名称（显示用），如 "性别", "订单状态" */
    @Column(name = "dict_name", nullable = false, length = 256)
    private String dictName;

    /** 字典结构类型: FLAT, TREE, ENUM */
    @Enumerated(EnumType.STRING)
    @Column(name = "dict_type", nullable = false, length = 16)
    private DictType dictType;

    @Column(length = 1024)
    private String description;

    /** 版本号，用于字典迁移和变更追踪 */
    @Column(length = 32)
    private String version;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // ===== 来源追溯 =====

    /** 来源类型: CRAWLED, MANUAL, IMPORTED */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private DictSourceType sourceType;

    /** 来源数据源 ID（nullable：手工创建的字典无数据源） */
    @Column(name = "datasource_id")
    private Long datasourceId;

    /** 来源 schema 名称 */
    @Column(name = "source_schema_name", length = 256)
    private String sourceSchemaName;

    /** 来源表名（字典数据所在的原始表，如 sys_dict、code_table 等） */
    @Column(name = "source_table_name", length = 256)
    private String sourceTableName;

    /** 来源说明（如导入文件名、外部系统名等） */
    @Column(name = "source_info", length = 512)
    private String sourceInfo;

    // ===== 时间戳 =====

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
