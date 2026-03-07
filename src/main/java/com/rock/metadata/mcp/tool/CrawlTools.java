package com.rock.metadata.mcp.tool;

import com.rock.metadata.model.CrawlJob;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.CrawlJobRepository;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CrawlTools {

    private final CrawlService crawlService;
    private final DataSourceConfigRepository dataSourceRepository;
    private final CrawlJobRepository crawlJobRepository;

    @McpTool(description = "Trigger an asynchronous metadata crawl for a datasource. " +
            "Returns immediately with a PENDING job. Use get_crawl_job_status to poll for completion.")
    public Map<String, Object> trigger_crawl(
            @McpToolParam(description = "Datasource ID to crawl") Long datasourceId,
            @McpToolParam(description = "Schema info level: minimum, standard, detailed, maximum (default: maximum)",
                    required = false) String infoLevel) {
        return ToolExecutor.run("trigger crawl", () -> {
            DataSourceConfig dsConfig = dataSourceRepository.findById(datasourceId)
                    .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + datasourceId));

            String level = (infoLevel != null && !infoLevel.isBlank()) ? infoLevel : "maximum";
            CrawlJob job = crawlService.createJob(datasourceId, level);
            crawlService.executeCrawl(job, dsConfig);
            return McpResponseHelper.compact(job);
        });
    }

    @McpTool(description = "Get the status and results of a crawl job. " +
            "Status will be PENDING, RUNNING, SUCCESS, or FAILED.")
    public Map<String, Object> get_crawl_job_status(
            @McpToolParam(description = "Crawl job ID") Long jobId) {
        return ToolExecutor.run("get crawl job status", () ->
                McpResponseHelper.compact(crawlJobRepository.findById(jobId)
                        .orElseThrow(() -> new IllegalArgumentException("CrawlJob not found: " + jobId))));
    }

    @McpTool(description = "List crawl jobs, optionally filtered by datasource ID. " +
            "Returns most recent jobs first, default limit 20.")
    public List<Map<String, Object>> list_crawl_jobs(
            @McpToolParam(description = "Datasource ID to filter by (optional)", required = false) Long datasourceId,
            @McpToolParam(description = "Max number of jobs to return (default 20)", required = false) Integer limit) {
        return ToolExecutor.run("list crawl jobs", () -> {
            int effectiveLimit = (limit != null && limit > 0) ? limit : 20;
            List<CrawlJob> jobs;
            if (datasourceId != null) {
                jobs = crawlJobRepository.findByDatasourceIdOrderByCreatedAtDesc(datasourceId);
            } else {
                jobs = crawlJobRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
            return jobs.stream()
                    .limit(effectiveLimit)
                    .map(McpResponseHelper::compact)
                    .toList();
        });
    }
}
