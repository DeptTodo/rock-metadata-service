package com.rock.metadata.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ds_config")
@Getter @Setter @NoArgsConstructor
public class DataSourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Database type: postgresql, mysql, oracle, sqlserver, sqlite, etc. */
    @Column(name = "db_type", nullable = false, length = 32)
    private String dbType;

    private String host;

    private Integer port;

    @Column(name = "database_name")
    private String databaseName;

    private String username;

    @JsonIgnore
    private String password;

    /** Optional: directly provide JDBC URL (overrides host/port/databaseName) */
    @Column(name = "jdbc_url", length = 1024)
    private String jdbcUrl;

    /** Comma-separated schema include patterns (regex), e.g. "public|sales" */
    @Column(name = "schema_patterns")
    private String schemaPatterns;

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
