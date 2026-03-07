package com.rock.metadata.dto;

import lombok.Data;
import java.util.List;

@Data
public class DistinctValueResponse {

    private Long tableId;
    private String tableName;
    private String columnName;
    private String dataType;
    private long totalDistinctCount;
    private List<ValueCount> values;

    @Data
    public static class ValueCount {
        private String value;
        private long count;
    }
}
