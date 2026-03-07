package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_node_filter",
        indexes = {
                @Index(name = "idx_filter_dataset_node", columnList = "dataset_id, node_code")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetNodeFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "node_code", nullable = false, length = 128)
    private String nodeCode;

    @Column(name = "filter_name", length = 256)
    private String filterName;

    @Column(name = "filter_expression", nullable = false, columnDefinition = "TEXT")
    private String filterExpression;

    private boolean parameterized = false;

    @Column(name = "param_name", length = 128)
    private String paramName;

    @Column(name = "param_type", length = 32)
    private String paramType;

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
