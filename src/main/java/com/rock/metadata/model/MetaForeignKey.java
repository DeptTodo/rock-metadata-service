package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_foreign_key", indexes = {
    @Index(name = "idx_meta_fk_table", columnList = "table_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaForeignKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "fk_name")
    private String fkName;

    @Column(name = "fk_column_name", nullable = false)
    private String fkColumnName;

    /** Referenced (primary key side) table full name */
    @Column(name = "pk_table_full_name", nullable = false)
    private String pkTableFullName;

    @Column(name = "pk_column_name", nullable = false)
    private String pkColumnName;

    @Column(name = "update_rule", length = 32)
    private String updateRule;

    @Column(name = "delete_rule", length = 32)
    private String deleteRule;
}
