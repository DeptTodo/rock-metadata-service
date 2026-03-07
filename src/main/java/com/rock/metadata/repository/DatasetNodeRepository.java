package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetNodeRepository extends JpaRepository<DatasetNode, Long> {

    List<DatasetNode> findByDatasetIdOrderByExecutionOrder(Long datasetId);

    Optional<DatasetNode> findByDatasetIdAndNodeCode(Long datasetId, String nodeCode);

    void deleteByDatasetId(Long datasetId);
}
