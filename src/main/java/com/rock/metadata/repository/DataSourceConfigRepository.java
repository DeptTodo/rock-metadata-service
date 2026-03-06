package com.rock.metadata.repository;

import com.rock.metadata.model.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfig, Long> {
}
