package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.DictDetailResponse;
import com.rock.metadata.model.DictColumnBinding;
import com.rock.metadata.model.DictDefinition;
import com.rock.metadata.model.DictItem;
import com.rock.metadata.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DictTools {

    private final DictService dictService;

    // ===== Dict Definition =====

    @Tool(description = "Create a new data dictionary definition")
    public DictDefinition create_dict(
            @ToolParam(description = "Unique dict code, e.g. GENDER, ORDER_STATUS") String dictCode,
            @ToolParam(description = "Display name for the dictionary") String dictName,
            @ToolParam(description = "Dict structure type: FLAT, TREE, or ENUM") String dictType,
            @ToolParam(description = "Description", required = false) String description,
            @ToolParam(description = "Version string", required = false) String version,
            @ToolParam(description = "Source type: CRAWLED, MANUAL, or IMPORTED") String sourceType,
            @ToolParam(description = "Source datasource ID (optional)", required = false) Long datasourceId,
            @ToolParam(description = "Source schema name (optional)", required = false) String sourceSchemaName,
            @ToolParam(description = "Source table name (optional)", required = false) String sourceTableName,
            @ToolParam(description = "Source info (optional)", required = false) String sourceInfo) {
        return dictService.createDict(dictCode, dictName, dictType, description, version,
                sourceType, datasourceId, sourceSchemaName, sourceTableName, sourceInfo);
    }

    @Tool(description = "List data dictionaries, optionally filtered by datasource or active status")
    public List<DictDefinition> list_dicts(
            @ToolParam(description = "Datasource ID (optional)", required = false) Long datasourceId,
            @ToolParam(description = "Only show active dicts (optional)", required = false) Boolean activeOnly) {
        return dictService.listDicts(datasourceId, activeOnly);
    }

    @Tool(description = "Get dictionary detail with all items by dict ID")
    public DictDetailResponse get_dict_detail(
            @ToolParam(description = "Dict ID") Long dictId) {
        return dictService.getDictDetail(dictId);
    }

    @Tool(description = "Get dictionary detail with all items by dict code")
    public DictDetailResponse get_dict_by_code(
            @ToolParam(description = "Dict code, e.g. GENDER") String dictCode) {
        return dictService.getDictByCode(dictCode);
    }

    @Tool(description = "Update a dictionary definition")
    public DictDefinition update_dict(
            @ToolParam(description = "Dict ID") Long dictId,
            @ToolParam(description = "New display name (optional)", required = false) String dictName,
            @ToolParam(description = "New description (optional)", required = false) String description,
            @ToolParam(description = "New version (optional)", required = false) String version,
            @ToolParam(description = "Active flag (optional)", required = false) Boolean active) {
        return dictService.updateDict(dictId, dictName, description, version, active);
    }

    @Tool(description = "Delete a dictionary and all its items and bindings")
    public String delete_dict(
            @ToolParam(description = "Dict ID") Long dictId) {
        dictService.deleteDict(dictId);
        return "Dict " + dictId + " deleted successfully";
    }

    // ===== Dict Items =====

    @Tool(description = "Add an item to a dictionary")
    public DictItem add_dict_item(
            @ToolParam(description = "Dict ID") Long dictId,
            @ToolParam(description = "Parent item ID for tree dicts (optional)", required = false) Long parentId,
            @ToolParam(description = "Item code (stored value), e.g. M, F, 1") String itemCode,
            @ToolParam(description = "Item display value, e.g. Male, Female") String itemValue,
            @ToolParam(description = "Item description (optional)", required = false) String itemDescription,
            @ToolParam(description = "Sort order (optional)", required = false) Integer sortOrder,
            @ToolParam(description = "Tree level depth (optional)", required = false) Integer treeLevel,
            @ToolParam(description = "Extended attributes as JSON (optional)", required = false) String extAttrs) {
        return dictService.addDictItem(dictId, parentId, itemCode, itemValue,
                itemDescription, sortOrder, treeLevel, extAttrs);
    }

    @Tool(description = "List items of a dictionary")
    public List<DictItem> list_dict_items(
            @ToolParam(description = "Dict ID") Long dictId,
            @ToolParam(description = "Only show active items (optional)", required = false) Boolean activeOnly) {
        return dictService.listDictItems(dictId, activeOnly);
    }

    @Tool(description = "Update a dictionary item")
    public DictItem update_dict_item(
            @ToolParam(description = "Item ID") Long itemId,
            @ToolParam(description = "New item code (optional)", required = false) String itemCode,
            @ToolParam(description = "New item value (optional)", required = false) String itemValue,
            @ToolParam(description = "New description (optional)", required = false) String itemDescription,
            @ToolParam(description = "New sort order (optional)", required = false) Integer sortOrder,
            @ToolParam(description = "Active flag (optional)", required = false) Boolean active) {
        return dictService.updateDictItem(itemId, itemCode, itemValue, itemDescription, sortOrder, active);
    }

    @Tool(description = "Delete a dictionary item")
    public String delete_dict_item(
            @ToolParam(description = "Item ID") Long itemId) {
        dictService.deleteDictItem(itemId);
        return "Dict item " + itemId + " deleted successfully";
    }

    // ===== Dict Bindings =====

    @Tool(description = "Bind a dictionary to a database column")
    public DictColumnBinding bind_dict_to_column(
            @ToolParam(description = "Dict ID") Long dictId,
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name (optional)", required = false) String schemaName,
            @ToolParam(description = "Table name") String tableName,
            @ToolParam(description = "Column name") String columnName,
            @ToolParam(description = "MetaColumn ID (optional)", required = false) Long metaColumnId,
            @ToolParam(description = "Binding type: MANUAL, NAME_MATCH, or LLM_INFERRED") String bindingType,
            @ToolParam(description = "Confidence score 0.0-1.0 (optional)", required = false) Double confidence) {
        return dictService.bindDictToColumn(dictId, datasourceId, schemaName, tableName,
                columnName, metaColumnId, bindingType, confidence);
    }

    @Tool(description = "List column bindings for a dictionary")
    public List<DictColumnBinding> list_dict_bindings(
            @ToolParam(description = "Dict ID") Long dictId) {
        return dictService.listDictBindings(dictId);
    }

    @Tool(description = "List dictionary bindings for a specific column")
    public List<DictColumnBinding> list_column_dict_bindings(
            @ToolParam(description = "MetaColumn ID") Long metaColumnId) {
        return dictService.listColumnDictBindings(metaColumnId);
    }

    @Tool(description = "Delete a dictionary-column binding")
    public String delete_dict_binding(
            @ToolParam(description = "Binding ID") Long bindingId) {
        dictService.deleteDictBinding(bindingId);
        return "Binding " + bindingId + " deleted successfully";
    }
}
