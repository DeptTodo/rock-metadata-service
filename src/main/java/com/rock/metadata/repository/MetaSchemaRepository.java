package com.rock.metadata.repository;

import com.rock.metadata.model.MetaSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaSchemaRepository extends JpaRepository<MetaSchema, Long> {

    List<MetaSchema> findByCrawlJobId(Long crawlJobId);

    void deleteByCrawlJobId(Long crawlJobId);
}
