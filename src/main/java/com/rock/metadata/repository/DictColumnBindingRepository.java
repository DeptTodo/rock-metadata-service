package com.rock.metadata.repository;

import com.rock.metadata.model.DictColumnBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictColumnBindingRepository extends JpaRepository<DictColumnBinding, Long> {

    List<DictColumnBinding> findByDictId(Long dictId);

    List<DictColumnBinding> findByDatasourceId(Long datasourceId);

    List<DictColumnBinding> findByMetaColumnId(Long metaColumnId);

    List<DictColumnBinding> findByDatasourceIdAndSchemaNameAndTableNameAndColumnName(
            Long datasourceId, String schemaName, String tableName, String columnName);

    void deleteByDictId(Long dictId);
}
