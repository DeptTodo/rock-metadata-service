package com.rock.metadata.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DatasourceSummary {

    private Long datasourceId;
    private int schemaCount;
    private int tableCount;
    private int columnCount;
    private int routineCount;
    private int sequenceCount;

    private Map<String, Integer> tableTypeDistribution;
    private List<TypeCount> columnTypeTop;
    private List<TableStat> tablesWithMostColumns;
    private List<TableStat> tablesWithMostIndexes;

    private LocalDateTime lastCrawlTime;
    private Long lastCrawlDurationMs;

    @Data
    public static class TableStat {
        private Long tableId;
        private String tableName;
        private String fullName;
        private int count;
    }

    @Data
    public static class TypeCount {
        private String type;
        private int count;
    }
}
