package com.rock.metadata.mcp.tool;

import com.rock.metadata.model.CrawlJob;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.CrawlJobRepository;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.rock.metadata.mcp.tool.McpResponseHelper.*;

@Component
@RequiredArgsConstructor
public class CrawlTools {

    private final CrawlService crawlService;
    private final DataSourceConfigRepository dataSourceRepository;
    private final CrawlJobRepository crawlJobRepository;

    @Tool(description = "Trigger an asynchronous metadata crawl for a datasource. " +
            "Returns immediately with a PENDING job. Use get_crawl_job_status to poll for completion.")
    public CrawlJob trigger_crawl(
            @ToolParam(description = "Datasource ID to crawl") Long datasourceId,
            @ToolParam(description = "Schema info level: minimum, standard, detailed, maximum (default: maximum)",
                    required = false) String infoLevel) {
        return ToolExecutor.run("trigger crawl", () -> {
            DataSourceConfig dsConfig = dataSourceRepository.findById(datasourceId)
                    .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + datasourceId));

            String level = (infoLevel != null && !infoLevel.isBlank()) ? infoLevel : "maximum";
            CrawlJob job = crawlService.createJob(datasourceId, level);
            crawlService.executeCrawl(job, dsConfig);
            return job;
        });
    }

    @Tool(description = "Get the status and results of a crawl job. " +
            "Status will be PENDING, RUNNING, SUCCESS, or FAILED.")
    public CrawlJob get_crawl_job_status(
            @ToolParam(description = "Crawl job ID") Long jobId) {
        return ToolExecutor.run("get crawl job status", () ->
                crawlJobRepository.findById(jobId)
                        .orElseThrow(() -> new IllegalArgumentException("CrawlJob not found: " + jobId)));
    }

    @Tool(description = "List crawl jobs, optionally filtered by datasource ID. " +
            "Returns most recent jobs first, default limit 20.")
    public List<Map<String, Object>> list_crawl_jobs(
            @ToolParam(description = "Datasource ID to filter by (optional)", required = false) Long datasourceId,
            @ToolParam(description = "Max number of jobs to return (default 20)", required = false) Integer limit) {
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
                    .map(this::toCrawlJobSummary)
                    .toList();
        });
    }

    private Map<String, Object> toCrawlJobSummary(CrawlJob job) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", job.getId());
        m.put("datasourceId", job.getDatasourceId());
        m.put("status", job.getStatus());
        m.put("infoLevel", job.getInfoLevel());
        m.put("tableCount", job.getTableCount());
        m.put("columnCount", job.getColumnCount());
        m.put("routineCount", job.getRoutineCount());
        m.put("errorMessage", truncate(job.getErrorMessage(), MEDIUM_TEXT));
        m.put("startedAt", job.getStartedAt());
        m.put("finishedAt", job.getFinishedAt());
        m.put("createdAt", job.getCreatedAt());
        return m;
    }
}
