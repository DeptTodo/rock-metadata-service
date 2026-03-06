package com.rock.metadata.repository;

import com.rock.metadata.model.MetaPrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaPrivilegeRepository extends JpaRepository<MetaPrivilege, Long> {

    List<MetaPrivilege> findByTableId(Long tableId);

    void deleteByTableIdIn(List<Long> tableIds);
}
