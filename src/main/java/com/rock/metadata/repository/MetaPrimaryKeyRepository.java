package com.rock.metadata.repository;

import com.rock.metadata.model.MetaPrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaPrimaryKeyRepository extends JpaRepository<MetaPrimaryKey, Long> {

    List<MetaPrimaryKey> findByTableId(Long tableId);

    void deleteByTableIdIn(List<Long> tableIds);
}
