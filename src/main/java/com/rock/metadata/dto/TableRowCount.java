package com.rock.metadata.dto;

import lombok.Data;

@Data
public class TableRowCount {
    private Long tableId;
    private String schemaName;
    private String tableName;
    private String fullName;
    private Long rowCount;
    private String error;
}
