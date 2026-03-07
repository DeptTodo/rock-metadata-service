package com.rock.metadata.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MetadataHealthResponse {

    private LocalDateTime lastCrawlTime;
    private String freshnessStatus;
    private int crawledTableCount;
    private Integer liveTableCount;
    private boolean connectionReachable;
    private String overallHealth;
    private List<String> warnings;
}
