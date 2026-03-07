package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.ImpactAnalysisResponse;
import com.rock.metadata.dto.TableRelationshipResponse;
import com.rock.metadata.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RelationshipTools {

    private final RelationshipService relationshipService;

    @Tool(description = "Get the FK relationship graph of a table (both outgoing and incoming), " +
            "traversed via BFS up to the specified depth (default 1, max 5)")
    public TableRelationshipResponse get_table_relationships(
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "Traversal depth (default 1, max 5)", required = false) Integer depth) {
        return ToolExecutor.run("get table relationships", () ->
                relationshipService.getTableRelationships(tableId, depth));
    }

    @Tool(description = "Analyze the cascade impact of modifying a table. " +
            "Shows directly and transitively affected tables via incoming FK references, " +
            "including update/delete rules.")
    public ImpactAnalysisResponse get_impact_analysis(
            @ToolParam(description = "Table ID") Long tableId) {
        return ToolExecutor.run("get impact analysis", () ->
                relationshipService.getImpactAnalysis(tableId));
    }
}
