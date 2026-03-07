package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_instance_snapshot",
        indexes = {
                @Index(name = "idx_snapshot_instance", columnList = "instance_id")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetInstanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    @Column(name = "node_code", length = 128)
    private String nodeCode;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "snapshot_hash", length = 64)
    private String snapshotHash;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
