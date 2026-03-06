package com.rock.metadata.repository;

import com.rock.metadata.model.MetaRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaRoutineRepository extends JpaRepository<MetaRoutine, Long> {

    List<MetaRoutine> findByDatasourceIdAndCrawlJobId(Long datasourceId, Long crawlJobId);
}
