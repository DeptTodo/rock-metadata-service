package com.rock.metadata.repository;

import com.rock.metadata.model.MetaTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaTriggerRepository extends JpaRepository<MetaTrigger, Long> {

    List<MetaTrigger> findByTableId(Long tableId);
}
