package com.rock.metadata.repository;

import com.rock.metadata.model.LlmAnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LlmAnalysisJobRepository extends JpaRepository<LlmAnalysisJob, Long> {

    List<LlmAnalysisJob> findByDatasourceIdOrderByCreatedAtDesc(Long datasourceId);
}
