package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetNodeFilterRequest {

    @NotNull
    private Long datasetId;

    @NotBlank
    private String nodeCode;

    private String filterName;

    @NotBlank
    private String filterExpression;

    private Boolean parameterized;

    private String paramName;

    private String paramType;

    private String defaultValue;

    private Boolean required;

    private Integer sortOrder;

    private Boolean enabled;
}
