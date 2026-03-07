package com.rock.metadata.dto;

import lombok.Data;
import java.util.List;

@Data
public class SchemaDiffResponse {

    private Long crawlJobId1;
    private Long crawlJobId2;
    private List<TableDiff> addedTables;
    private List<TableDiff> removedTables;
    private List<TableModification> modifiedTables;
    private DiffSummary summary;

    @Data
    public static class TableDiff {
        private String fullName;
        private String tableName;
        private String schemaName;
        private String tableType;
    }

    @Data
    public static class TableModification {
        private String fullName;
        private String tableName;
        private List<ColumnDiff> addedColumns;
        private List<ColumnDiff> removedColumns;
        private List<ColumnModification> modifiedColumns;
    }

    @Data
    public static class ColumnDiff {
        private String columnName;
        private String dataType;
        private boolean nullable;
    }

    @Data
    public static class ColumnModification {
        private String columnName;
        private List<PropertyChange> changes;
    }

    @Data
    public static class PropertyChange {
        private String property;
        private String oldValue;
        private String newValue;
    }

    @Data
    public static class DiffSummary {
        private int addedTableCount;
        private int removedTableCount;
        private int modifiedTableCount;
        private int totalAddedColumns;
        private int totalRemovedColumns;
        private int totalModifiedColumns;
    }
}
