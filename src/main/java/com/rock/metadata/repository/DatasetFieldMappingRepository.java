package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetFieldMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetFieldMappingRepository extends JpaRepository<DatasetFieldMapping, Long> {

    List<DatasetFieldMapping> findByDatasetIdAndNodeCodeOrderBySortOrder(Long datasetId, String nodeCode);

    List<DatasetFieldMapping> findByDatasetId(Long datasetId);

    void deleteByDatasetId(Long datasetId);
}
