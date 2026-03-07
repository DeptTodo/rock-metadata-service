package com.rock.metadata.repository;

import com.rock.metadata.model.MetaRoutineColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaRoutineColumnRepository extends JpaRepository<MetaRoutineColumn, Long> {

    List<MetaRoutineColumn> findByRoutineId(Long routineId);

    List<MetaRoutineColumn> findByRoutineIdOrderByOrdinalPosition(Long routineId);

    void deleteByRoutineIdIn(List<Long> routineIds);
}
