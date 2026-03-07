package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.SchemaDiffResponse;
import com.rock.metadata.service.SchemaDiffService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaDiffTools {

    private final SchemaDiffService schemaDiffService;

    @McpTool(description = "Compare schema changes between two crawls of a datasource. " +
            "Shows added/removed/modified tables and columns with property-level changes. " +
            "If crawlJobId1 and crawlJobId2 are not provided, compares the last two successful crawls.")
    public SchemaDiffResponse compare_crawls(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Older crawl job ID (optional, defaults to second-latest)", required = false) Long crawlJobId1,
            @McpToolParam(description = "Newer crawl job ID (optional, defaults to latest)", required = false) Long crawlJobId2) {
        return ToolExecutor.run("compare crawls", () ->
                schemaDiffService.compareCrawls(datasourceId, crawlJobId1, crawlJobId2));
    }
}
