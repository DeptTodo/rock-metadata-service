package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DataSourceRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String dbType;

    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private String jdbcUrl;
    private String schemaPatterns;
}
