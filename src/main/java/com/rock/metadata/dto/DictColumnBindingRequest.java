package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictColumnBindingRequest {

    @NotNull
    private Long dictId;

    @NotNull
    private Long datasourceId;

    private String schemaName;

    @NotBlank
    private String tableName;

    @NotBlank
    private String columnName;

    private Long metaColumnId;

    @NotBlank
    private String bindingType;

    private Double confidence;
}
