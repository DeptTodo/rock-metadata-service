package com.rock.metadata.controller;

import com.rock.metadata.dto.CrawlRequest;
import com.rock.metadata.model.CrawlJob;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.CrawlJobRepository;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.service.CrawlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlService crawlService;
    private final DataSourceConfigRepository dataSourceRepository;
    private final CrawlJobRepository crawlJobRepository;

    /** Trigger a metadata crawl for a datasource (async). */
    @PostMapping("/datasources/{datasourceId}/crawl")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public CrawlJob triggerCrawl(@PathVariable Long datasourceId,
                                 @RequestBody(required = false) CrawlRequest request) {
        DataSourceConfig dsConfig = dataSourceRepository.findById(datasourceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));

        String infoLevel = request != null ? request.getInfoLevel() : "maximum";
        CrawlJob job = crawlService.createJob(datasourceId, infoLevel);

        // Execute asynchronously
        crawlService.executeCrawl(job, dsConfig);

        return job;
    }

    @GetMapping("/crawl-jobs")
    public List<CrawlJob> listJobs(
            @RequestParam(required = false) Long datasourceId) {
        if (datasourceId != null) {
            return crawlJobRepository.findByDatasourceIdOrderByCreatedAtDesc(datasourceId);
        }
        return crawlJobRepository.findAll();
    }

    @GetMapping("/crawl-jobs/{id}")
    public CrawlJob getJob(@PathVariable Long id) {
        return crawlJobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CrawlJob not found"));
    }
}
