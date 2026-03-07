package com.rock.metadata.repository;

import com.rock.metadata.model.MetaTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MetaTableRepository extends JpaRepository<MetaTable, Long>, JpaSpecificationExecutor<MetaTable> {

    List<MetaTable> findByCrawlJobId(Long crawlJobId);

    List<MetaTable> findByCrawlJobIdAndSchemaName(Long crawlJobId, String schemaName);

    @Query("SELECT t FROM MetaTable t WHERE t.crawlJobId = :crawlJobId " +
           "AND (LOWER(t.tableName) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(t.remarks) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    List<MetaTable> searchByKeyword(Long crawlJobId, String keyword);

    void deleteByCrawlJobId(Long crawlJobId);
}
