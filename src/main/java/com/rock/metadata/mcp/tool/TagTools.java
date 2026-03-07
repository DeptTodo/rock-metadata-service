package com.rock.metadata.mcp.tool;

import com.rock.metadata.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TagTools {

    private final TagService tagService;

    @Tool(description = "Create a tag on a metadata entity (SCHEMA, TABLE, or COLUMN)")
    public Map<String, Object> create_tag(
            @ToolParam(description = "Target type: SCHEMA, TABLE, or COLUMN") String targetType,
            @ToolParam(description = "ID of the target entity") Long targetId,
            @ToolParam(description = "Tag key") String tagKey,
            @ToolParam(description = "Tag value (optional)", required = false) String tagValue,
            @ToolParam(description = "Tag source: MANUAL, LLM, or CRAWLER (default: MANUAL)", required = false) String source) {
        return ToolExecutor.run("create tag", () ->
                McpResponseHelper.compact(tagService.createTag(targetType, targetId, tagKey, tagValue, source)));
    }

    @Tool(description = "List all tags for a specific metadata entity")
    public List<Map<String, Object>> list_tags_by_target(
            @ToolParam(description = "Target type: SCHEMA, TABLE, or COLUMN") String targetType,
            @ToolParam(description = "ID of the target entity") Long targetId) {
        return ToolExecutor.run("list tags", () ->
                tagService.listTagsByTarget(targetType, targetId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @Tool(description = "List tags by key, optionally filtered by value")
    public List<Map<String, Object>> list_tags_by_key(
            @ToolParam(description = "Tag key to search for") String tagKey,
            @ToolParam(description = "Tag value to filter by (optional)", required = false) String tagValue) {
        return ToolExecutor.run("list tags by key", () ->
                tagService.listTagsByKey(tagKey, tagValue).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @Tool(description = "Update an existing tag")
    public Map<String, Object> update_tag(
            @ToolParam(description = "Tag ID") Long tagId,
            @ToolParam(description = "New tag key (optional)", required = false) String tagKey,
            @ToolParam(description = "New tag value (optional)", required = false) String tagValue,
            @ToolParam(description = "New source (optional)", required = false) String source) {
        return ToolExecutor.run("update tag", () ->
                McpResponseHelper.compact(tagService.updateTag(tagId, tagKey, tagValue, source)));
    }

    @Tool(description = "Delete a single tag by ID")
    public String delete_tag(
            @ToolParam(description = "Tag ID") Long tagId) {
        ToolExecutor.runVoid("delete tag", () -> tagService.deleteTag(tagId));
        return "Tag " + tagId + " deleted successfully";
    }

    @Tool(description = "Delete all tags for a specific metadata entity")
    public String delete_tags_by_target(
            @ToolParam(description = "Target type: SCHEMA, TABLE, or COLUMN") String targetType,
            @ToolParam(description = "ID of the target entity") Long targetId) {
        ToolExecutor.runVoid("delete tags", () -> tagService.deleteTagsByTarget(targetType, targetId));
        return "Tags deleted for " + targetType + " " + targetId;
    }
}
