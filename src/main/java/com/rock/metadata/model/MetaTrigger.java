package com.rock.metadata.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "meta_trigger", indexes = {
    @Index(name = "idx_meta_trigger_table", columnList = "table_id")
})
@Getter @Setter @NoArgsConstructor
public class MetaTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(name = "trigger_name")
    private String triggerName;

    /** BEFORE, AFTER, INSTEAD_OF */
    @Column(name = "condition_timing", length = 32)
    private String conditionTiming;

    /** INSERT, UPDATE, DELETE */
    @Column(name = "event_manipulation_type", length = 32)
    private String eventManipulationType;

    /** ROW, STATEMENT */
    @Column(name = "action_orientation", length = 16)
    private String actionOrientation;

    @Column(name = "action_condition", columnDefinition = "TEXT")
    private String actionCondition;

    @Column(name = "action_statement", columnDefinition = "TEXT")
    private String actionStatement;

    @Column(name = "action_order")
    private int actionOrder;
}
