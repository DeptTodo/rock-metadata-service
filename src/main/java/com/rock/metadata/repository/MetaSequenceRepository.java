package com.rock.metadata.repository;

import com.rock.metadata.model.MetaSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaSequenceRepository extends JpaRepository<MetaSequence, Long> {

    List<MetaSequence> findByDatasourceIdAndCrawlJobId(Long datasourceId, Long crawlJobId);

    List<MetaSequence> findByCrawlJobId(Long crawlJobId);

    List<MetaSequence> findByCrawlJobIdAndSchemaName(Long crawlJobId, String schemaName);

    void deleteByCrawlJobId(Long crawlJobId);
}
