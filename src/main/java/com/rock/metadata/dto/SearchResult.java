package com.rock.metadata.dto;

import com.rock.metadata.model.MetaColumn;
import com.rock.metadata.model.MetaTable;
import lombok.Data;
import java.util.List;

@Data
public class SearchResult {

    private List<MetaTable> tables;
    private List<ColumnMatch> columns;

    @Data
    public static class ColumnMatch {
        private String tableFullName;
        private MetaColumn column;
    }
}
