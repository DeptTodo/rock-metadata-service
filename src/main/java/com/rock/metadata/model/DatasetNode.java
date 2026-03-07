package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_node",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dataset_node_code", columnNames = {"dataset_id", "node_code"})
        },
        indexes = {
                @Index(name = "idx_node_dataset", columnList = "dataset_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "node_code", nullable = false, length = 128)
    private String nodeCode;

    @Column(name = "node_name", nullable = false, length = 256)
    private String nodeName;

    @Column(name = "source_schema", length = 256)
    private String sourceSchema;

    @Column(name = "source_table", nullable = false, length = 256)
    private String sourceTable;

    @Column(name = "node_type", nullable = false, length = 32)
    private String nodeType;

    @Column(name = "parent_node_code", length = 128)
    private String parentNodeCode;

    @Column(name = "execution_order")
    private Integer executionOrder = 0;

    @Column(length = 16)
    private String cardinality;

    @Column(name = "max_rows")
    private Integer maxRows = 10000;

    @Column(length = 32)
    private String source = "MANUAL";

    private Double confidence;

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
