package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ColumnQualityRuleRequest {

    @NotNull
    private Long ruleId;

    @NotNull
    private Long datasourceId;

    private String schemaName;

    @NotBlank
    private String tableName;

    @NotBlank
    private String columnName;

    private Long metaColumnId;

    private String severity;

    private String params;

    private Boolean enabled;
}
