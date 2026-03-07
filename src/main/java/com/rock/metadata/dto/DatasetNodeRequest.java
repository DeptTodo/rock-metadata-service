package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetNodeRequest {

    @NotNull
    private Long datasetId;

    @NotBlank
    private String nodeCode;

    @NotBlank
    private String nodeName;

    private String sourceSchema;

    @NotBlank
    private String sourceTable;

    @NotBlank
    private String nodeType;

    private String parentNodeCode;

    private Integer executionOrder;

    private String cardinality;

    private Integer maxRows;

    private String source;

    private Double confidence;

    private Boolean enabled;
}
