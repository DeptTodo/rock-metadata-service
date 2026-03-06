package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_constraint", indexes = {
    @Index(name = "idx_meta_constraint_table", columnList = "table_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaConstraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "constraint_name")
    private String constraintName;

    /** CHECK, UNIQUE, NOT_NULL, etc. */
    @Column(name = "constraint_type", length = 32)
    private String constraintType;

    /** Comma-separated column names */
    @Column(name = "column_names", length = 4096)
    private String columnNames;

    /** Constraint definition / expression */
    @Column(columnDefinition = "TEXT")
    private String definition;

    private boolean deferrable;

    @Column(name = "initially_deferred")
    private boolean initiallyDeferred;
}
