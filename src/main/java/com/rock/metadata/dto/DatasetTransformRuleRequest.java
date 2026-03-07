package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DatasetTransformRuleRequest {

    @NotBlank
    private String ruleCode;

    @NotBlank
    private String ruleName;

    @NotBlank
    private String ruleType;

    @NotBlank
    private String ruleContent;

    private String description;

    private Boolean active;
}
