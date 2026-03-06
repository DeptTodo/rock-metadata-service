package com.rock.metadata.repository;

import com.rock.metadata.model.MetaForeignKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaForeignKeyRepository extends JpaRepository<MetaForeignKey, Long> {

    List<MetaForeignKey> findByTableId(Long tableId);

    void deleteByTableIdIn(List<Long> tableIds);
}
