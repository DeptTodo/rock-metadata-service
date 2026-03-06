package com.rock.metadata.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableRowCount {
    private Long tableId;
    private String schemaName;
    private String tableName;
    private String fullName;
    private Long rowCount;
    private String error;
}
