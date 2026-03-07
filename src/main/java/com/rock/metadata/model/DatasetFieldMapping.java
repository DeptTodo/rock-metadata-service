package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_field_mapping",
        indexes = {
                @Index(name = "idx_mapping_dataset_node", columnList = "dataset_id, node_code")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "node_code", nullable = false, length = 128)
    private String nodeCode;

    @Column(name = "source_field", nullable = false, length = 256)
    private String sourceField;

    @Column(name = "output_field", nullable = false, length = 256)
    private String outputField;

    @Column(name = "output_type", length = 32)
    private String outputType;

    @Column(name = "transform_rule_id")
    private Long transformRuleId;

    @Column(name = "inline_expression", length = 1024)
    private String inlineExpression;

    @Column(name = "default_value", length = 512)
    private String defaultValue;

    private boolean required = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
