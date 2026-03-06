package com.rock.metadata.repository;

import com.rock.metadata.model.CrawlJob;
import com.rock.metadata.model.CrawlStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CrawlJobRepository extends JpaRepository<CrawlJob, Long> {

    List<CrawlJob> findByDatasourceIdOrderByCreatedAtDesc(Long datasourceId);

    Optional<CrawlJob> findFirstByDatasourceIdAndStatusOrderByFinishedAtDesc(
            Long datasourceId, CrawlStatus status);
}
