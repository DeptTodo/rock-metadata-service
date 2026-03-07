package com.rock.metadata.mcp.tool;

import com.rock.metadata.model.MetaTag;
import com.rock.metadata.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TagTools {

    private final TagService tagService;

    @Tool(description = "Create a tag on a metadata entity (SCHEMA, TABLE, or COLUMN)")
    public MetaTag create_tag(
            @ToolParam(description = "Target type: SCHEMA, TABLE, or COLUMN") String targetType,
            @ToolParam(description = "ID of the target entity") Long targetId,
            @ToolParam(description = "Tag key") String tagKey,
            @ToolParam(description = "Tag value (optional)", required = false) String tagValue,
            @ToolParam(description = "Tag source: MANUAL, LLM, or CRAWLER (default: MANUAL)", required = false) String source) {
        return tagService.createTag(targetType, targetId, tagKey, tagValue, source);
    }

    @Tool(description = "List all tags for a specific metadata entity")
    public List<MetaTag> list_tags_by_target(
            @ToolParam(description = "Target type: SCHEMA, TABLE, or COLUMN") String targetType,
            @ToolParam(description = "ID of the target entity") Long targetId) {
        return tagService.listTagsByTarget(targetType, targetId);
    }

    @Tool(description = "List tags by key, optionally filtered by value")
    public List<MetaTag> list_tags_by_key(
            @ToolParam(description = "Tag key to search for") String tagKey,
            @ToolParam(description = "Tag value to filter by (optional)", required = false) String tagValue) {
        return tagService.listTagsByKey(tagKey, tagValue);
    }

    @Tool(description = "Update an existing tag")
    public MetaTag update_tag(
            @ToolParam(description = "Tag ID") Long tagId,
            @ToolParam(description = "New tag key (optional)", required = false) String tagKey,
            @ToolParam(description = "New tag value (optional)", required = false) String tagValue,
            @ToolParam(description = "New source (optional)", required = false) String source) {
        return tagService.updateTag(tagId, tagKey, tagValue, source);
    }

    @Tool(description = "Delete a single tag by ID")
    public String delete_tag(
            @ToolParam(description = "Tag ID") Long tagId) {
        tagService.deleteTag(tagId);
        return "Tag " + tagId + " deleted successfully";
    }

    @Tool(description = "Delete all tags for a specific metadata entity")
    public String delete_tags_by_target(
            @ToolParam(description = "Target type: SCHEMA, TABLE, or COLUMN") String targetType,
            @ToolParam(description = "ID of the target entity") Long targetId) {
        tagService.deleteTagsByTarget(targetType, targetId);
        return "Tags deleted for " + targetType + " " + targetId;
    }
}
