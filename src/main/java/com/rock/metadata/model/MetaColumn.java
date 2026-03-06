package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_column", indexes = {
    @Index(name = "idx_meta_column_table", columnList = "table_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "ordinal_position")
    private int ordinalPosition;

    @Column(name = "data_type", length = 128)
    private String dataType;

    @Column(name = "column_size")
    private int columnSize;

    @Column(name = "decimal_digits")
    private int decimalDigits;

    private boolean nullable;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "auto_incremented")
    private boolean autoIncremented;

    private boolean generated;

    @Column(name = "part_of_primary_key")
    private boolean partOfPrimaryKey;

    @Column(name = "part_of_foreign_key")
    private boolean partOfForeignKey;

    @Column(name = "part_of_index")
    private boolean partOfIndex;

    @Column(length = 4096)
    private String remarks;
}
