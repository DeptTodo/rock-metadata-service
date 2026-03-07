package com.rock.metadata.controller;

import com.rock.metadata.dto.DictColumnBindingRequest;
import com.rock.metadata.dto.DictDefinitionRequest;
import com.rock.metadata.dto.DictDetailResponse;
import com.rock.metadata.dto.DictItemRequest;
import com.rock.metadata.model.DictColumnBinding;
import com.rock.metadata.model.DictDefinition;
import com.rock.metadata.model.DictItem;
import com.rock.metadata.service.DictService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ===== Dict Definition =====

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DictDefinition createDict(@Valid @RequestBody DictDefinitionRequest req) {
        return dictService.createDict(req.getDictCode(), req.getDictName(), req.getDictType(),
                req.getDescription(), req.getVersion(), req.getSourceType(),
                req.getDatasourceId(), req.getSourceSchemaName(),
                req.getSourceTableName(), req.getSourceInfo());
    }

    @GetMapping
    public List<DictDefinition> listDicts(
            @RequestParam(required = false) Long datasourceId,
            @RequestParam(required = false) Boolean activeOnly) {
        return dictService.listDicts(datasourceId, activeOnly);
    }

    @GetMapping("/{dictId}")
    public DictDetailResponse getDictDetail(@PathVariable Long dictId) {
        return dictService.getDictDetail(dictId);
    }

    @GetMapping("/by-code/{dictCode}")
    public DictDetailResponse getDictByCode(@PathVariable String dictCode) {
        return dictService.getDictByCode(dictCode);
    }

    @PutMapping("/{dictId}")
    public DictDefinition updateDict(
            @PathVariable Long dictId,
            @RequestBody DictDefinitionRequest req) {
        return dictService.updateDict(dictId, req.getDictName(), req.getDescription(),
                req.getVersion(), req.getActive());
    }

    @DeleteMapping("/{dictId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDict(@PathVariable Long dictId) {
        dictService.deleteDict(dictId);
    }

    // ===== Dict Items =====

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public DictItem addDictItem(@Valid @RequestBody DictItemRequest req) {
        return dictService.addDictItem(req.getDictId(), req.getParentId(), req.getItemCode(),
                req.getItemValue(), req.getItemDescription(), req.getSortOrder(),
                req.getTreeLevel(), req.getExtAttrs());
    }

    @GetMapping("/{dictId}/items")
    public List<DictItem> listDictItems(
            @PathVariable Long dictId,
            @RequestParam(required = false) Boolean activeOnly) {
        return dictService.listDictItems(dictId, activeOnly);
    }

    @PutMapping("/items/{itemId}")
    public DictItem updateDictItem(
            @PathVariable Long itemId,
            @RequestBody DictItemRequest req) {
        return dictService.updateDictItem(itemId, req.getItemCode(), req.getItemValue(),
                req.getItemDescription(), req.getSortOrder(), req.getActive());
    }

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDictItem(@PathVariable Long itemId) {
        dictService.deleteDictItem(itemId);
    }

    // ===== Dict Bindings =====

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public DictColumnBinding bindDictToColumn(@Valid @RequestBody DictColumnBindingRequest req) {
        return dictService.bindDictToColumn(req.getDictId(), req.getDatasourceId(),
                req.getSchemaName(), req.getTableName(), req.getColumnName(),
                req.getMetaColumnId(), req.getBindingType(), req.getConfidence());
    }

    @GetMapping("/{dictId}/bindings")
    public List<DictColumnBinding> listDictBindings(@PathVariable Long dictId) {
        return dictService.listDictBindings(dictId);
    }

    @GetMapping("/bindings/by-column/{metaColumnId}")
    public List<DictColumnBinding> listColumnDictBindings(@PathVariable Long metaColumnId) {
        return dictService.listColumnDictBindings(metaColumnId);
    }

    @DeleteMapping("/bindings/{bindingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDictBinding(@PathVariable Long bindingId) {
        dictService.deleteDictBinding(bindingId);
    }
}
