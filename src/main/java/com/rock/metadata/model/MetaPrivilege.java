package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_privilege", indexes = {
    @Index(name = "idx_meta_privilege_table", columnList = "table_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaPrivilege {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    /** SELECT, INSERT, UPDATE, DELETE, REFERENCES, TRIGGER, etc. */
    @Column(name = "privilege_type", length = 64)
    private String privilegeType;

    @Column(length = 128)
    private String grantor;

    @Column(length = 128)
    private String grantee;

    @Column(name = "is_grantable")
    private boolean grantable;
}
