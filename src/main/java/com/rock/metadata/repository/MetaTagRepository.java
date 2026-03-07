package com.rock.metadata.repository;

import com.rock.metadata.model.MetaTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MetaTagRepository extends JpaRepository<MetaTag, Long> {

    List<MetaTag> findByTargetTypeAndTargetId(String targetType, Long targetId);

    List<MetaTag> findByTagKey(String tagKey);

    List<MetaTag> findByTagKeyAndTagValue(String tagKey, String tagValue);

    List<MetaTag> findByTargetTypeAndTargetIdIn(String targetType, List<Long> targetIds);

    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);
}
