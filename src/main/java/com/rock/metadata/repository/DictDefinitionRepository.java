package com.rock.metadata.repository;

import com.rock.metadata.model.DictDefinition;
import com.rock.metadata.model.DictSourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DictDefinitionRepository extends JpaRepository<DictDefinition, Long> {

    Optional<DictDefinition> findByDictCode(String dictCode);

    List<DictDefinition> findByDatasourceId(Long datasourceId);

    List<DictDefinition> findBySourceType(DictSourceType sourceType);

    List<DictDefinition> findByDatasourceIdAndSourceSchemaName(Long datasourceId, String sourceSchemaName);

    List<DictDefinition> findByActiveTrue();

    boolean existsByDictCode(String dictCode);
}
