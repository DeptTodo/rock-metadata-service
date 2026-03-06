package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_routine_column", indexes = {
    @Index(name = "idx_meta_routine_col_routine", columnList = "routine_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaRoutineColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "routine_id", nullable = false)
    private Long routineId;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "ordinal_position")
    private int ordinalPosition;

    /** IN, OUT, INOUT, RETURN, RESULT */
    @Column(name = "column_type", length = 16)
    private String columnType;

    @Column(name = "data_type", length = 128)
    private String dataType;

    @Column(name = "precision_value")
    private int precision;

    @Column(name = "scale_value")
    private int scale;

    @Column(name = "is_nullable")
    private boolean nullable;
}
