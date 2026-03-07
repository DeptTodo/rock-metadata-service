package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetNodeRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetNodeRelationRepository extends JpaRepository<DatasetNodeRelation, Long> {

    List<DatasetNodeRelation> findByDatasetId(Long datasetId);

    List<DatasetNodeRelation> findByDatasetIdAndChildNodeCode(Long datasetId, String childNodeCode);

    List<DatasetNodeRelation> findByDatasetIdAndParentNodeCode(Long datasetId, String parentNodeCode);

    void deleteByDatasetId(Long datasetId);
}
