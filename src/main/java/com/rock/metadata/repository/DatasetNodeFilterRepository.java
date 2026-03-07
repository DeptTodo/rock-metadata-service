package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetNodeFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetNodeFilterRepository extends JpaRepository<DatasetNodeFilter, Long> {

    List<DatasetNodeFilter> findByDatasetIdAndNodeCodeOrderBySortOrder(Long datasetId, String nodeCode);

    List<DatasetNodeFilter> findByDatasetId(Long datasetId);

    void deleteByDatasetId(Long datasetId);
}
