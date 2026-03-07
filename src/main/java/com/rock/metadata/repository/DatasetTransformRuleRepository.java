package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetTransformRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetTransformRuleRepository extends JpaRepository<DatasetTransformRule, Long> {

    Optional<DatasetTransformRule> findByRuleCode(String ruleCode);

    List<DatasetTransformRule> findByActiveTrue();

    boolean existsByRuleCode(String ruleCode);
}
