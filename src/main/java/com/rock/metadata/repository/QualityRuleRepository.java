package com.rock.metadata.repository;

import com.rock.metadata.model.QualityRule;
import com.rock.metadata.model.QualityRuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QualityRuleRepository extends JpaRepository<QualityRule, Long> {

    Optional<QualityRule> findByRuleCode(String ruleCode);

    boolean existsByRuleCode(String ruleCode);

    List<QualityRule> findByRuleType(QualityRuleType ruleType);

    List<QualityRule> findByActiveTrue();

    List<QualityRule> findByBuiltInTrue();

    List<QualityRule> findByBuiltInFalse();
}
