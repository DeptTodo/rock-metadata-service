package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.SchemaDiffResponse;
import com.rock.metadata.service.SchemaDiffService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchemaDiffTools {

    private final SchemaDiffService schemaDiffService;

    @Tool(description = "Compare schema changes between two crawls of a datasource. " +
            "Shows added/removed/modified tables and columns with property-level changes. " +
            "If crawlJobId1 and crawlJobId2 are not provided, compares the last two successful crawls.")
    public SchemaDiffResponse compare_crawls(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Older crawl job ID (optional, defaults to second-latest)", required = false) Long crawlJobId1,
            @ToolParam(description = "Newer crawl job ID (optional, defaults to latest)", required = false) Long crawlJobId2) {
        return schemaDiffService.compareCrawls(datasourceId, crawlJobId1, crawlJobId2);
    }
}
