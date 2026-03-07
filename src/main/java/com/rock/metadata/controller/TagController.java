package com.rock.metadata.controller;

import com.rock.metadata.dto.MetaTagRequest;
import com.rock.metadata.model.MetaTag;
import com.rock.metadata.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetaTag create(@Valid @RequestBody MetaTagRequest req) {
        return tagService.createTag(req.getTargetType(), req.getTargetId(),
                req.getTagKey(), req.getTagValue(), req.getSource());
    }

    @GetMapping("/by-target")
    public List<MetaTag> listByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return tagService.listTagsByTarget(targetType, targetId);
    }

    @GetMapping("/by-key")
    public List<MetaTag> listByKey(
            @RequestParam String tagKey,
            @RequestParam(required = false) String tagValue) {
        return tagService.listTagsByKey(tagKey, tagValue);
    }

    @PutMapping("/{tagId}")
    public MetaTag update(
            @PathVariable Long tagId,
            @RequestBody MetaTagRequest req) {
        return tagService.updateTag(tagId, req.getTagKey(), req.getTagValue(), req.getSource());
    }

    @DeleteMapping("/{tagId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
    }

    @DeleteMapping("/by-target")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        tagService.deleteTagsByTarget(targetType, targetId);
    }
}
