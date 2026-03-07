package com.rock.metadata.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImpactAnalysisResponse {

    private String tableName;
    private String tableFullName;
    private List<AffectedTable> directlyAffected;
    private List<AffectedTable> transitivelyAffected;
    private int totalAffectedCount;

    @Data
    public static class AffectedTable {
        private String tableName;
        private String tableFullName;
        private String fkName;
        private String fkColumn;
        private String pkColumn;
        private String updateRule;
        private String deleteRule;
        private int depth;
    }
}
