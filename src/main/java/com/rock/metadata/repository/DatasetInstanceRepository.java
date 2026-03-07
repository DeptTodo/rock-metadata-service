package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetExecutionStatus;
import com.rock.metadata.model.DatasetInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetInstanceRepository extends JpaRepository<DatasetInstance, Long> {

    List<DatasetInstance> findByDatasetIdOrderByCreatedAtDesc(Long datasetId);

    List<DatasetInstance> findByDatasetIdAndExecutionStatusOrderByCreatedAtDesc(
            Long datasetId, DatasetExecutionStatus status);

    Optional<DatasetInstance> findFirstByDatasetIdAndExecutionStatusOrderByFinishedAtDesc(
            Long datasetId, DatasetExecutionStatus status);

    void deleteByDatasetId(Long datasetId);
}
