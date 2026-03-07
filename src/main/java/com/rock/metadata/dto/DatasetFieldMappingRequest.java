package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatasetFieldMappingRequest {

    @NotNull
    private Long datasetId;

    @NotBlank
    private String nodeCode;

    @NotBlank
    private String sourceField;

    @NotBlank
    private String outputField;

    private String outputType;

    private Long transformRuleId;

    private String inlineExpression;

    private String defaultValue;

    private Boolean required;

    private Integer sortOrder;

    private Boolean enabled;
}
