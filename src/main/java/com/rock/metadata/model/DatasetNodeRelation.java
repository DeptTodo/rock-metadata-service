package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_node_relation",
        indexes = {
                @Index(name = "idx_relation_dataset", columnList = "dataset_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetNodeRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "parent_node_code", nullable = false, length = 128)
    private String parentNodeCode;

    @Column(name = "child_node_code", nullable = false, length = 128)
    private String childNodeCode;

    @Column(name = "relation_type", length = 32)
    private String relationType;

    @Column(name = "parent_join_column", length = 256)
    private String parentJoinColumn;

    @Column(name = "child_join_column", length = 256)
    private String childJoinColumn;

    @Column(name = "join_expression", columnDefinition = "TEXT")
    private String joinExpression;

    @Column(name = "join_mode", length = 16)
    private String joinMode = "LEFT";

    @Column(name = "dependency_level")
    private Integer dependencyLevel = 0;

    @Column(name = "max_depth")
    private Integer maxDepth = 1;

    @Column(length = 32)
    private String source = "MANUAL";

    private Double confidence;

    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
