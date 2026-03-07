package com.rock.metadata.dto;

import lombok.Data;
import java.util.List;

@Data
public class TableRelationshipResponse {

    private TableNode rootTable;
    private List<RelationshipEdge> edges;
    private int depth;

    @Data
    public static class TableNode {
        private Long tableId;
        private String tableName;
        private String fullName;
        private String schemaName;
    }

    @Data
    public static class RelationshipEdge {
        private String fromTable;
        private String toTable;
        private String fkName;
        private String fromColumn;
        private String toColumn;
        private String direction;
        private int depth;
    }
}
