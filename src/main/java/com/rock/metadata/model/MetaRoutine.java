package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_routine", indexes = {
    @Index(name = "idx_meta_routine_ds_job", columnList = "datasource_id, crawl_job_id"),
    @Index(name = "idx_meta_routine_schema", columnList = "schema_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    @Column(name = "crawl_job_id", nullable = false)
    private Long crawlJobId;

    @Column(name = "schema_id")
    private Long schemaId;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "routine_name", nullable = false)
    private String routineName;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "specific_name")
    private String specificName;

    /** PROCEDURE, FUNCTION, UNKNOWN */
    @Column(name = "routine_type", length = 32)
    private String routineType;

    /** Return data type (for functions) */
    @Column(name = "return_type", length = 128)
    private String returnType;

    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(length = 4096)
    private String remarks;
}
