package com.rock.metadata.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DataSampleResponse {

    private Long tableId;
    private String tableName;
    private String fullName;
    private int limit;
    private long totalRowCount;
    private List<String> columnNames;
    private List<Map<String, Object>> rows;
}
