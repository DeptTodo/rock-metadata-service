package com.rock.metadata.repository;

import com.rock.metadata.model.MetaColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MetaColumnRepository extends JpaRepository<MetaColumn, Long>, JpaSpecificationExecutor<MetaColumn> {

    List<MetaColumn> findByTableIdOrderByOrdinalPosition(Long tableId);

    @Query("SELECT c FROM MetaColumn c WHERE c.tableId IN :tableIds " +
           "AND (LOWER(c.columnName) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(c.remarks) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    List<MetaColumn> searchByKeyword(List<Long> tableIds, String keyword);

    void deleteByTableIdIn(List<Long> tableIds);
}
