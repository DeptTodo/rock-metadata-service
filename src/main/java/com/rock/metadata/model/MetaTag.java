package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_tag", indexes = {
    @Index(name = "idx_meta_tag_target", columnList = "target_type, target_id"),
    @Index(name = "idx_meta_tag_key", columnList = "tag_key")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"target_type", "target_id", "tag_key", "tag_value"})
})
@Getter @Setter @NoArgsConstructor
public class MetaTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Target type: SCHEMA, TABLE, COLUMN */
    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;

    /** ID of the target entity (schema_id, table_id, or column_id) */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "tag_key", nullable = false, length = 128)
    private String tagKey;

    @Column(name = "tag_value", length = 512)
    private String tagValue;

    /** Source of this tag: MANUAL, LLM, CRAWLER */
    @Column(length = 32)
    private String source;
}
