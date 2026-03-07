package com.rock.metadata.dto;

import lombok.Data;
import java.util.List;

@Data
public class TableProfileResponse {

    private Long tableId;
    private String tableName;
    private String fullName;
    private long rowCount;
    private List<ColumnProfile> columnProfiles;
}
