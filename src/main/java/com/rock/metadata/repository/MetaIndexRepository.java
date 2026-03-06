package com.rock.metadata.repository;

import com.rock.metadata.model.MetaIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaIndexRepository extends JpaRepository<MetaIndex, Long> {

    List<MetaIndex> findByTableId(Long tableId);

    void deleteByTableIdIn(List<Long> tableIds);
}
