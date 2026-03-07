package com.rock.metadata.dto;

import lombok.Data;

@Data
public class AdvancedSearchRequest {

    // Table filters
    private String schemaName;
    private String tableType;
    private String importanceLevel;
    private String businessDomain;
    private String tableNamePattern;

    // Column filters
    private String dataType;
    private String sensitivityLevel;
    private Boolean nullable;
    private Boolean partOfPrimaryKey;
    private Boolean partOfForeignKey;
    private String columnNamePattern;
}
