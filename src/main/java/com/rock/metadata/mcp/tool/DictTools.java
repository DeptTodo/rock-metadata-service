package com.rock.metadata.mcp.tool;

import com.rock.metadata.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.rock.metadata.mcp.tool.McpResponseHelper.*;

@Component
@RequiredArgsConstructor
public class DictTools {

    private final DictService dictService;

    // ===== Dict Definition =====

    @McpTool(description = "Create a new data dictionary definition")
    public Map<String, Object> create_dict(
            @McpToolParam(description = "Unique dict code, e.g. GENDER, ORDER_STATUS") String dictCode,
            @McpToolParam(description = "Display name for the dictionary") String dictName,
            @McpToolParam(description = "Dict structure type: FLAT, TREE, or ENUM") String dictType,
            @McpToolParam(description = "Description", required = false) String description,
            @McpToolParam(description = "Version string", required = false) String version,
            @McpToolParam(description = "Source type: CRAWLED, MANUAL, or IMPORTED") String sourceType,
            @McpToolParam(description = "Source datasource ID (optional)", required = false) Long datasourceId,
            @McpToolParam(description = "Source schema name (optional)", required = false) String sourceSchemaName,
            @McpToolParam(description = "Source table name (optional)", required = false) String sourceTableName,
            @McpToolParam(description = "Source info (optional)", required = false) String sourceInfo) {
        return ToolExecutor.run("create dict", () ->
                compact(dictService.createDict(dictCode, dictName, dictType, description, version,
                        sourceType, datasourceId, sourceSchemaName, sourceTableName, sourceInfo)));
    }

    @McpTool(description = "List data dictionaries, optionally filtered by datasource or active status")
    public List<Map<String, Object>> list_dicts(
            @McpToolParam(description = "Datasource ID (optional)", required = false) Long datasourceId,
            @McpToolParam(description = "Only show active dicts (optional)", required = false) Boolean activeOnly) {
        return ToolExecutor.run("list dicts", () ->
                dictService.listDicts(datasourceId, activeOnly).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Get dictionary detail with all items by dict ID")
    public Map<String, Object> get_dict_detail(
            @McpToolParam(description = "Dict ID") Long dictId) {
        return ToolExecutor.run("get dict detail", () ->
                compactDictDetail(dictService.getDictDetail(dictId)));
    }

    @McpTool(description = "Get dictionary detail with all items by dict code")
    public Map<String, Object> get_dict_by_code(
            @McpToolParam(description = "Dict code, e.g. GENDER") String dictCode) {
        return ToolExecutor.run("get dict by code", () ->
                compactDictDetail(dictService.getDictByCode(dictCode)));
    }

    @McpTool(description = "Update a dictionary definition")
    public Map<String, Object> update_dict(
            @McpToolParam(description = "Dict ID") Long dictId,
            @McpToolParam(description = "New display name (optional)", required = false) String dictName,
            @McpToolParam(description = "New description (optional)", required = false) String description,
            @McpToolParam(description = "New version (optional)", required = false) String version,
            @McpToolParam(description = "Active flag (optional)", required = false) Boolean active) {
        return ToolExecutor.run("update dict", () ->
                compact(dictService.updateDict(dictId, dictName, description, version, active)));
    }

    @McpTool(description = "Delete a dictionary and all its items and bindings")
    public String delete_dict(
            @McpToolParam(description = "Dict ID") Long dictId) {
        ToolExecutor.runVoid("delete dict", () -> dictService.deleteDict(dictId));
        return "Dict " + dictId + " deleted successfully";
    }

    // ===== Dict Items =====

    @McpTool(description = "Add an item to a dictionary")
    public Map<String, Object> add_dict_item(
            @McpToolParam(description = "Dict ID") Long dictId,
            @McpToolParam(description = "Parent item ID for tree dicts (optional)", required = false) Long parentId,
            @McpToolParam(description = "Item code (stored value), e.g. M, F, 1") String itemCode,
            @McpToolParam(description = "Item display value, e.g. Male, Female") String itemValue,
            @McpToolParam(description = "Item description (optional)", required = false) String itemDescription,
            @McpToolParam(description = "Sort order (optional)", required = false) Integer sortOrder,
            @McpToolParam(description = "Tree level depth (optional)", required = false) Integer treeLevel,
            @McpToolParam(description = "Extended attributes as JSON (optional)", required = false) String extAttrs) {
        return ToolExecutor.run("add dict item", () ->
                compact(dictService.addDictItem(dictId, parentId, itemCode, itemValue,
                        itemDescription, sortOrder, treeLevel, extAttrs)));
    }

    @McpTool(description = "List items of a dictionary")
    public List<Map<String, Object>> list_dict_items(
            @McpToolParam(description = "Dict ID") Long dictId,
            @McpToolParam(description = "Only show active items (optional)", required = false) Boolean activeOnly) {
        return ToolExecutor.run("list dict items", () ->
                dictService.listDictItems(dictId, activeOnly).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Update a dictionary item")
    public Map<String, Object> update_dict_item(
            @McpToolParam(description = "Item ID") Long itemId,
            @McpToolParam(description = "New item code (optional)", required = false) String itemCode,
            @McpToolParam(description = "New item value (optional)", required = false) String itemValue,
            @McpToolParam(description = "New description (optional)", required = false) String itemDescription,
            @McpToolParam(description = "New sort order (optional)", required = false) Integer sortOrder,
            @McpToolParam(description = "Active flag (optional)", required = false) Boolean active) {
        return ToolExecutor.run("update dict item", () ->
                compact(dictService.updateDictItem(itemId, itemCode, itemValue, itemDescription, sortOrder, active)));
    }

    @McpTool(description = "Delete a dictionary item")
    public String delete_dict_item(
            @McpToolParam(description = "Item ID") Long itemId) {
        ToolExecutor.runVoid("delete dict item", () -> dictService.deleteDictItem(itemId));
        return "Dict item " + itemId + " deleted successfully";
    }

    // ===== Dict Bindings =====

    @McpTool(description = "Bind a dictionary to a database column")
    public Map<String, Object> bind_dict_to_column(
            @McpToolParam(description = "Dict ID") Long dictId,
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @McpToolParam(description = "Table name") String tableName,
            @McpToolParam(description = "Column name") String columnName,
            @McpToolParam(description = "MetaColumn ID (optional)", required = false) Long metaColumnId,
            @McpToolParam(description = "Binding type: MANUAL, NAME_MATCH, or LLM_INFERRED") String bindingType,
            @McpToolParam(description = "Confidence score 0.0-1.0 (optional)", required = false) Double confidence) {
        return ToolExecutor.run("bind dict to column", () ->
                compact(dictService.bindDictToColumn(dictId, datasourceId, schemaName, tableName,
                        columnName, metaColumnId, bindingType, confidence)));
    }

    @McpTool(description = "List column bindings for a dictionary")
    public List<Map<String, Object>> list_dict_bindings(
            @McpToolParam(description = "Dict ID") Long dictId) {
        return ToolExecutor.run("list dict bindings", () ->
                dictService.listDictBindings(dictId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "List dictionary bindings for a specific column")
    public List<Map<String, Object>> list_column_dict_bindings(
            @McpToolParam(description = "MetaColumn ID") Long metaColumnId) {
        return ToolExecutor.run("list column dict bindings", () ->
                dictService.listColumnDictBindings(metaColumnId).stream()
                        .map(McpResponseHelper::compact).toList());
    }

    @McpTool(description = "Delete a dictionary-column binding")
    public String delete_dict_binding(
            @McpToolParam(description = "Binding ID") Long bindingId) {
        ToolExecutor.runVoid("delete dict binding", () -> dictService.deleteDictBinding(bindingId));
        return "Binding " + bindingId + " deleted successfully";
    }
}
