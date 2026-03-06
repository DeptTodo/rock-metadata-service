package com.rock.metadata.repository;

import com.rock.metadata.model.DictItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DictItemRepository extends JpaRepository<DictItem, Long> {

    List<DictItem> findByDictIdOrderBySortOrder(Long dictId);

    List<DictItem> findByDictIdAndActiveTrue(Long dictId);

    List<DictItem> findByDictIdAndParentIdIsNullOrderBySortOrder(Long dictId);

    List<DictItem> findByDictIdAndParentIdOrderBySortOrder(Long dictId, Long parentId);

    Optional<DictItem> findByDictIdAndItemCode(Long dictId, String itemCode);

    void deleteByDictId(Long dictId);
}
