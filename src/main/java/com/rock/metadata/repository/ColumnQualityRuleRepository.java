package com.rock.metadata.repository;

import com.rock.metadata.model.ColumnQualityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColumnQualityRuleRepository extends JpaRepository<ColumnQualityRule, Long> {

    List<ColumnQualityRule> findByRuleId(Long ruleId);

    List<ColumnQualityRule> findByDatasourceId(Long datasourceId);

    List<ColumnQualityRule> findByMetaColumnId(Long metaColumnId);

    List<ColumnQualityRule> findByDatasourceIdAndSchemaNameAndTableNameAndColumnName(
            Long datasourceId, String schemaName, String tableName, String columnName);

    List<ColumnQualityRule> findByDatasourceIdAndSchemaNameAndTableName(
            Long datasourceId, String schemaName, String tableName);

    List<ColumnQualityRule> findByMetaColumnIdIn(List<Long> metaColumnIds);

    void deleteByRuleId(Long ruleId);
}
