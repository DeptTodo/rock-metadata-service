package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_instance",
        indexes = {
                @Index(name = "idx_instance_dataset", columnList = "dataset_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    @Column(name = "dataset_version", nullable = false)
    private Integer datasetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 32)
    private DatasetExecutionStatus executionStatus = DatasetExecutionStatus.PENDING;

    @Column(name = "execution_params", columnDefinition = "TEXT")
    private String executionParams;

    @Column(name = "root_key_value", length = 512)
    private String rootKeyValue;

    @Column(name = "total_nodes")
    private Integer totalNodes = 0;

    @Column(name = "success_nodes")
    private Integer successNodes = 0;

    @Column(name = "failed_nodes")
    private Integer failedNodes = 0;

    @Column(name = "total_rows")
    private Long totalRows = 0L;

    @Column(name = "error_message", length = 4096)
    private String errorMessage;

    @Column(name = "node_progress", columnDefinition = "TEXT")
    private String nodeProgress;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
