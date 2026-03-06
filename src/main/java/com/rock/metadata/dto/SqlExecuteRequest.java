package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SqlExecuteRequest {

    @NotNull
    private Long datasourceId;

    @NotBlank
    private String sql;
}
