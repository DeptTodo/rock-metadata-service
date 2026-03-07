package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dataset_transform_rule",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transform_rule_code", columnNames = "rule_code")
        })
@Getter
@Setter
@NoArgsConstructor
public class DatasetTransformRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 128)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 256)
    private String ruleName;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    @Column(name = "rule_content", nullable = false, columnDefinition = "TEXT")
    private String ruleContent;

    @Column(length = 1024)
    private String description;

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
