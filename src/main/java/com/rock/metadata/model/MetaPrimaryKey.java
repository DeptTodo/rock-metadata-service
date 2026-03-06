package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_primary_key", indexes = {
    @Index(name = "idx_meta_pk_table", columnList = "table_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaPrimaryKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "constraint_name")
    private String constraintName;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "key_sequence")
    private int keySequence;
}
