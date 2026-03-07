package com.rock.metadata.dto;

import com.rock.metadata.model.MetaColumn;
import com.rock.metadata.model.MetaTable;
import lombok.Data;
import java.util.List;

@Data
public class AdvancedSearchResponse {

    private List<MetaTable> tables;
    private List<ColumnResult> columns;
    private int tableCount;
    private int columnCount;

    @Data
    public static class ColumnResult {
        private String tableFullName;
        private MetaColumn column;
    }
}
