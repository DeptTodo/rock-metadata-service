package com.rock.metadata.mcp.tool;

import com.rock.metadata.model.CrawlJob;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.CrawlJobRepository;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

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

        DataSourceConfig dsConfig = dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + datasourceId));

        String level = (infoLevel != null && !infoLevel.isBlank()) ? infoLevel : "maximum";
        CrawlJob job = crawlService.createJob(datasourceId, level);
        crawlService.executeCrawl(job, dsConfig);
        return job;
    }

    @Tool(description = "Get the status and results of a crawl job. " +
            "Status will be PENDING, RUNNING, SUCCESS, or FAILED.")
    public CrawlJob get_crawl_job_status(
            @ToolParam(description = "Crawl job ID") Long jobId) {
        return crawlJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("CrawlJob not found: " + jobId));
    }

    @Tool(description = "List crawl jobs, optionally filtered by datasource ID")
    public List<CrawlJob> list_crawl_jobs(
            @ToolParam(description = "Datasource ID to filter by (optional)", required = false) Long datasourceId) {
        if (datasourceId != null) {
            return crawlJobRepository.findByDatasourceIdOrderByCreatedAtDesc(datasourceId);
        }
        return crawlJobRepository.findAll();
    }
}
