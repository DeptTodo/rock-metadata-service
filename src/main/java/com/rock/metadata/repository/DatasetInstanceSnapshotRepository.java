package com.rock.metadata.repository;

import com.rock.metadata.model.DatasetInstanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetInstanceSnapshotRepository extends JpaRepository<DatasetInstanceSnapshot, Long> {

    List<DatasetInstanceSnapshot> findByInstanceId(Long instanceId);

    Optional<DatasetInstanceSnapshot> findByInstanceIdAndNodeCode(Long instanceId, String nodeCode);

    Optional<DatasetInstanceSnapshot> findByInstanceIdAndNodeCodeIsNull(Long instanceId);

    void deleteByInstanceId(Long instanceId);
}
