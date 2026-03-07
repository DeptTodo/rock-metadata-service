package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetNodeRelationRequest {

    @NotNull
    private Long datasetId;

    @NotBlank
    private String parentNodeCode;

    @NotBlank
    private String childNodeCode;

    private String relationType;

    private String parentJoinColumn;

    private String childJoinColumn;

    private String joinExpression;

    private String joinMode;

    private Integer dependencyLevel;

    private Integer maxDepth;

    private String source;

    private Double confidence;

    private Boolean enabled;
}
