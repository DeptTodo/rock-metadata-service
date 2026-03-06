package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 数据字典项 — 字典中的每一个取值条目。
 * 支持树形层级（parent_id）和扩展属性（extAttrs）。
 */
@Entity
@Table(name = "dict_item", indexes = {
    @Index(name = "idx_dict_item_dict", columnList = "dict_id"),
    @Index(name = "idx_dict_item_parent", columnList = "parent_id"),
    @Index(name = "idx_dict_item_code", columnList = "dict_id, item_code")
})
@Getter @Setter @NoArgsConstructor
public class DictItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属字典定义 */
    @Column(name = "dict_id", nullable = false)
    private Long dictId;

    /** 父项 ID（树形字典时使用，顶层为 null） */
    @Column(name = "parent_id")
    private Long parentId;

    /** 编码值（存储在业务表中的实际值），如 "M", "F", "1", "001" */
    @Column(name = "item_code", nullable = false, length = 256)
    private String itemCode;

    /** 显示值 / 标签，如 "男", "女", "待支付" */
    @Column(name = "item_value", nullable = false, length = 512)
    private String itemValue;

    /** 项的描述或备注 */
    @Column(name = "item_description", length = 1024)
    private String itemDescription;

    /** 排序序号 */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 树形层级深度（0 = 顶层） */
    @Column(name = "tree_level")
    private Integer treeLevel;

    /** 扩展属性（JSON 格式），用于存储不同风格字典的额外字段 */
    @Column(name = "ext_attrs", columnDefinition = "TEXT")
    private String extAttrs;

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
