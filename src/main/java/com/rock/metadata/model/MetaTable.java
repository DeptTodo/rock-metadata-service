package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_table", indexes = {
    @Index(name = "idx_meta_table_ds_job", columnList = "datasource_id, crawl_job_id"),
    @Index(name = "idx_meta_table_schema", columnList = "schema_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaTable {

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

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    /** TABLE, VIEW, MATERIALIZED VIEW, SYSTEM TABLE, etc. */
    @Column(name = "table_type", length = 64)
    private String tableType;

    @Column(length = 4096)
    private String remarks;

    /** DDL definition for views */
    @Column(columnDefinition = "TEXT")
    private String definition;

    @Column(name = "row_count")
    private Long rowCount;
}
