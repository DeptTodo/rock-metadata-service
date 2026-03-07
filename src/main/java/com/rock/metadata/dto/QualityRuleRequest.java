package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QualityRuleRequest {

    @NotBlank
    private String ruleCode;

    @NotBlank
    private String ruleName;

    @NotNull
    private String ruleType;

    private String description;

    @NotNull
    private String defaultSeverity;

    private String defaultParams;

    private Boolean active;
}
