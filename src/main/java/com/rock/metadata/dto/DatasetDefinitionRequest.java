package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetDefinitionRequest {

    @NotBlank
    private String datasetCode;

    @NotBlank
    private String datasetName;

    private String description;

    private String businessDomain;

    @NotNull
    private Long datasourceId;

    private String outputFormat;

    private String rootNodeCode;

    private Integer maxExecutionTimeSeconds;

    private String owner;
}
