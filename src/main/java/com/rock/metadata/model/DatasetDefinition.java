package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_definition",
        indexes = {
                @Index(name = "idx_dataset_ds", columnList = "datasource_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dataset_code", columnNames = "dataset_code")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_code", nullable = false, length = 128)
    private String datasetCode;

    @Column(name = "dataset_name", nullable = false, length = 256)
    private String datasetName;

    @Column(length = 1024)
    private String description;

    @Column(name = "business_domain", length = 128)
    private String businessDomain;

    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    @Column(nullable = false)
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DatasetStatus status = DatasetStatus.DRAFT;

    @Column(name = "output_format", length = 16)
    private String outputFormat = "TREE";

    @Column(name = "root_node_code", length = 128)
    private String rootNodeCode;

    @Column(name = "max_execution_time_seconds")
    private Integer maxExecutionTimeSeconds = 120;

    @Column(length = 128)
    private String owner;

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
