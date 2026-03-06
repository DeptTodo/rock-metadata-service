package com.rock.metadata.repository;

import com.rock.metadata.model.MetaConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaConstraintRepository extends JpaRepository<MetaConstraint, Long> {

    List<MetaConstraint> findByTableId(Long tableId);
}
