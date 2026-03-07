package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictDefinitionRequest {

    @NotBlank
    private String dictCode;

    @NotBlank
    private String dictName;

    @NotNull
    private String dictType;

    private String description;
    private String version;
    private Boolean active;

    @NotNull
    private String sourceType;

    private Long datasourceId;
    private String sourceSchemaName;
    private String sourceTableName;
    private String sourceInfo;
}
