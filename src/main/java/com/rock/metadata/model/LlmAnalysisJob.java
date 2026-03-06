package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_analysis_job", indexes = {
    @Index(name = "idx_llm_job_ds", columnList = "datasource_id")
})
@Getter @Setter @NoArgsConstructor
public class LlmAnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    /** Scope: DATASOURCE, SCHEMA, TABLE */
    @Column(name = "analysis_scope", nullable = false, length = 32)
    private String analysisScope;

    /** ID of the scoped entity (null if scope is DATASOURCE) */
    @Column(name = "scope_target_id")
    private Long scopeTargetId;

    /** LLM model used, e.g. "claude-opus-4-6" */
    @Column(name = "model_name", length = 128)
    private String modelName;

    /** What aspects were analyzed: BUSINESS, SECURITY, ALL */
    @Column(name = "analysis_type", length = 64)
    private String analysisType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CrawlStatus status;

    @Column(name = "error_message", length = 4096)
    private String errorMessage;

    /** Number of tables analyzed */
    @Column(name = "tables_analyzed")
    private Integer tablesAnalyzed;

    /** Number of columns analyzed */
    @Column(name = "columns_analyzed")
    private Integer columnsAnalyzed;

    /** Total tokens consumed */
    @Column(name = "total_tokens")
    private Long totalTokens;

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
