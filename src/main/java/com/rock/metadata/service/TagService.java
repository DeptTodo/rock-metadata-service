package com.rock.metadata.service;

import com.rock.metadata.model.MetaTag;
import com.rock.metadata.repository.MetaTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final MetaTagRepository metaTagRepository;

    @Transactional
    public MetaTag createTag(String targetType, Long targetId, String tagKey, String tagValue, String source) {
        MetaTag tag = new MetaTag();
        tag.setTargetType(targetType);
        tag.setTargetId(targetId);
        tag.setTagKey(tagKey);
        tag.setTagValue(tagValue);
        tag.setSource(source != null ? source : "MANUAL");
        return metaTagRepository.save(tag);
    }

    @Transactional(readOnly = true)
    public List<MetaTag> listTagsByTarget(String targetType, Long targetId) {
        return metaTagRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Transactional(readOnly = true)
    public List<MetaTag> listTagsByKey(String tagKey, String tagValue) {
        if (tagValue != null && !tagValue.isBlank()) {
            return metaTagRepository.findByTagKeyAndTagValue(tagKey, tagValue);
        }
        return metaTagRepository.findByTagKey(tagKey);
    }

    @Transactional
    public MetaTag updateTag(Long tagId, String tagKey, String tagValue, String source) {
        MetaTag tag = metaTagRepository.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tag not found: " + tagId));
        if (tagKey != null) tag.setTagKey(tagKey);
        if (tagValue != null) tag.setTagValue(tagValue);
        if (source != null) tag.setSource(source);
        return metaTagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long tagId) {
        if (!metaTagRepository.existsById(tagId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found: " + tagId);
        }
        metaTagRepository.deleteById(tagId);
    }

    @Transactional
    public void deleteTagsByTarget(String targetType, Long targetId) {
        metaTagRepository.deleteByTargetTypeAndTargetId(targetType, targetId);
    }
}
