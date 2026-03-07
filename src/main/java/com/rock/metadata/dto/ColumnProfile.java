package com.rock.metadata.dto;

import lombok.Data;
import java.util.List;

@Data
public class ColumnProfile {

    private String columnName;
    private String dataType;
    private long distinctCount;
    private long nullCount;
    private double nullPercentage;
    private String minValue;
    private String maxValue;
    private List<String> sampleValues;
}
