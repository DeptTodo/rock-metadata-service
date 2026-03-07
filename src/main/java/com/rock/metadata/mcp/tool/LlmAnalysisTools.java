package com.rock.metadata.mcp.tool;

import com.rock.metadata.service.MetadataQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LlmAnalysisTools {

    private final MetadataQueryService metadataQueryService;

    @Tool(description = "List LLM analysis jobs, optionally filtered by datasource")
    public List<Map<String, Object>> list_llm_analysis_jobs(
            @ToolParam(description = "Datasource ID (optional)", required = false) Long datasourceId) {
        return ToolExecutor.run("list LLM analysis jobs", () ->
                metadataQueryService.listLlmAnalysisJobs(datasourceId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @Tool(description = "Get LLM analysis job details and status")
    public Map<String, Object> get_llm_analysis_job(
            @ToolParam(description = "LLM analysis job ID") Long jobId) {
        return ToolExecutor.run("get LLM analysis job", () ->
                McpResponseHelper.compact(metadataQueryService.getLlmAnalysisJob(jobId)));
    }
}
