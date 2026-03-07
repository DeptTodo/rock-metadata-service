package com.rock.metadata.mcp.tool;

import com.rock.metadata.model.LlmAnalysisJob;
import com.rock.metadata.service.MetadataQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmAnalysisTools {

    private final MetadataQueryService metadataQueryService;

    @Tool(description = "List LLM analysis jobs, optionally filtered by datasource")
    public List<LlmAnalysisJob> list_llm_analysis_jobs(
            @ToolParam(description = "Datasource ID (optional)", required = false) Long datasourceId) {
        return metadataQueryService.listLlmAnalysisJobs(datasourceId);
    }

    @Tool(description = "Get LLM analysis job details and status")
    public LlmAnalysisJob get_llm_analysis_job(
            @ToolParam(description = "LLM analysis job ID") Long jobId) {
        return metadataQueryService.getLlmAnalysisJob(jobId);
    }
}
