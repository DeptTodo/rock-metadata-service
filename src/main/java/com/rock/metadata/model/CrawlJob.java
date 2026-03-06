package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_job")
@Getter @Setter @NoArgsConstructor
public class CrawlJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CrawlStatus status;

    /** SchemaCrawler info level: minimum, standard, detailed, maximum */
    @Column(name = "info_level", length = 16)
    private String infoLevel;

    @Column(name = "error_message", length = 4096)
    private String errorMessage;

    @Column(name = "table_count")
    private Integer tableCount;

    @Column(name = "column_count")
    private Integer columnCount;

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
