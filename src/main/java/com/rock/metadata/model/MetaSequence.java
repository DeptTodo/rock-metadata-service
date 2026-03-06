package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigInteger;

@Entity
@Table(name = "meta_sequence", indexes = {
    @Index(name = "idx_meta_sequence_ds_job", columnList = "datasource_id, crawl_job_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    @Column(name = "crawl_job_id", nullable = false)
    private Long crawlJobId;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "sequence_name", nullable = false)
    private String sequenceName;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "start_value")
    private BigInteger startValue;

    private long increment;

    @Column(name = "minimum_value")
    private BigInteger minimumValue;

    @Column(name = "maximum_value")
    private BigInteger maximumValue;

    @Column(name = "is_cycle")
    private boolean cycle;

    @Column(length = 4096)
    private String remarks;
}
