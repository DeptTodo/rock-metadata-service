package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_index", indexes = {
    @Index(name = "idx_meta_index_table", columnList = "table_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "index_name")
    private String indexName;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "ordinal_position")
    private int ordinalPosition;

    @Column(name = "index_type", length = 32)
    private String indexType;

    @Column(name = "is_unique")
    private boolean unique;
}
