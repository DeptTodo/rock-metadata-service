package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetDefinition;
import com.rock.metadata.model.DatasetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetDefinitionRepository extends JpaRepository<DatasetDefinition, Long> {

    Optional<DatasetDefinition> findByDatasetCode(String datasetCode);

    List<DatasetDefinition> findByDatasourceId(Long datasourceId);

    List<DatasetDefinition> findByStatus(DatasetStatus status);

    List<DatasetDefinition> findByDatasourceIdAndStatus(Long datasourceId, DatasetStatus status);

    List<DatasetDefinition> findByBusinessDomain(String businessDomain);

    boolean existsByDatasetCode(String datasetCode);
}
