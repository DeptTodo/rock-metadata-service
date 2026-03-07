package com.rock.metadata.service;

import com.rock.metadata.dto.DictDetailResponse;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.DictColumnBindingRepository;
import com.rock.metadata.repository.DictDefinitionRepository;
import com.rock.metadata.repository.DictItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictService {

    private final DictDefinitionRepository dictDefinitionRepository;
    private final DictItemRepository dictItemRepository;
    private final DictColumnBindingRepository dictColumnBindingRepository;

    // ===== Dict Definition =====

    @Transactional
    public DictDefinition createDict(String dictCode, String dictName, String dictType,
                                      String description, String version, String sourceType,
                                      Long datasourceId, String sourceSchemaName,
                                      String sourceTableName, String sourceInfo) {
        if (dictDefinitionRepository.existsByDictCode(dictCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dict code already exists: " + dictCode);
        }
        DictDefinition dict = new DictDefinition();
        dict.setDictCode(dictCode);
        dict.setDictName(dictName);
        dict.setDictType(DictType.valueOf(dictType));
        dict.setDescription(description);
        dict.setVersion(version);
        dict.setSourceType(DictSourceType.valueOf(sourceType));
        dict.setDatasourceId(datasourceId);
        dict.setSourceSchemaName(sourceSchemaName);
        dict.setSourceTableName(sourceTableName);
        dict.setSourceInfo(sourceInfo);
        return dictDefinitionRepository.save(dict);
    }

    @Transactional(readOnly = true)
    public List<DictDefinition> listDicts(Long datasourceId, Boolean activeOnly) {
        if (activeOnly != null && activeOnly) {
            return dictDefinitionRepository.findByActiveTrue();
        }
        if (datasourceId != null) {
            return dictDefinitionRepository.findByDatasourceId(datasourceId);
        }
        return dictDefinitionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public DictDetailResponse getDictDetail(Long dictId) {
        DictDefinition dict = dictDefinitionRepository.findById(dictId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dict not found: " + dictId));
        DictDetailResponse resp = new DictDetailResponse();
        resp.setDefinition(dict);
        resp.setItems(dictItemRepository.findByDictIdOrderBySortOrder(dictId));
        return resp;
    }

    @Transactional(readOnly = true)
    public DictDetailResponse getDictByCode(String dictCode) {
        DictDefinition dict = dictDefinitionRepository.findByDictCode(dictCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dict not found: " + dictCode));
        DictDetailResponse resp = new DictDetailResponse();
        resp.setDefinition(dict);
        resp.setItems(dictItemRepository.findByDictIdOrderBySortOrder(dict.getId()));
        return resp;
    }

    @Transactional
    public DictDefinition updateDict(Long dictId, String dictName, String description,
                                      String version, Boolean active) {
        DictDefinition dict = dictDefinitionRepository.findById(dictId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dict not found: " + dictId));
        if (dictName != null) dict.setDictName(dictName);
        if (description != null) dict.setDescription(description);
        if (version != null) dict.setVersion(version);
        if (active != null) dict.setActive(active);
        return dictDefinitionRepository.save(dict);
    }

    @Transactional
    public void deleteDict(Long dictId) {
        if (!dictDefinitionRepository.existsById(dictId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dict not found: " + dictId);
        }
        dictItemRepository.deleteByDictId(dictId);
        dictColumnBindingRepository.deleteByDictId(dictId);
        dictDefinitionRepository.deleteById(dictId);
    }

    // ===== Dict Items =====

    @Transactional
    public DictItem addDictItem(Long dictId, Long parentId, String itemCode, String itemValue,
                                 String itemDescription, Integer sortOrder, Integer treeLevel,
                                 String extAttrs) {
        if (!dictDefinitionRepository.existsById(dictId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dict not found: " + dictId);
        }
        DictItem item = new DictItem();
        item.setDictId(dictId);
        item.setParentId(parentId);
        item.setItemCode(itemCode);
        item.setItemValue(itemValue);
        item.setItemDescription(itemDescription);
        item.setSortOrder(sortOrder != null ? sortOrder : 0);
        item.setTreeLevel(treeLevel != null ? treeLevel : 0);
        item.setExtAttrs(extAttrs);
        return dictItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<DictItem> listDictItems(Long dictId, Boolean activeOnly) {
        if (activeOnly != null && activeOnly) {
            return dictItemRepository.findByDictIdAndActiveTrue(dictId);
        }
        return dictItemRepository.findByDictIdOrderBySortOrder(dictId);
    }

    @Transactional
    public DictItem updateDictItem(Long itemId, String itemCode, String itemValue,
                                    String itemDescription, Integer sortOrder, Boolean active) {
        DictItem item = dictItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dict item not found: " + itemId));
        if (itemCode != null) item.setItemCode(itemCode);
        if (itemValue != null) item.setItemValue(itemValue);
        if (itemDescription != null) item.setItemDescription(itemDescription);
        if (sortOrder != null) item.setSortOrder(sortOrder);
        if (active != null) item.setActive(active);
        return dictItemRepository.save(item);
    }

    @Transactional
    public void deleteDictItem(Long itemId) {
        if (!dictItemRepository.existsById(itemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dict item not found: " + itemId);
        }
        dictItemRepository.deleteById(itemId);
    }

    // ===== Dict Column Bindings =====

    @Transactional
    public DictColumnBinding bindDictToColumn(Long dictId, Long datasourceId, String schemaName,
                                               String tableName, String columnName,
                                               Long metaColumnId, String bindingType,
                                               Double confidence) {
        if (!dictDefinitionRepository.existsById(dictId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dict not found: " + dictId);
        }
        DictColumnBinding binding = new DictColumnBinding();
        binding.setDictId(dictId);
        binding.setDatasourceId(datasourceId);
        binding.setSchemaName(schemaName);
        binding.setTableName(tableName);
        binding.setColumnName(columnName);
        binding.setMetaColumnId(metaColumnId);
        binding.setBindingType(bindingType);
        binding.setConfidence(confidence);
        return dictColumnBindingRepository.save(binding);
    }

    @Transactional(readOnly = true)
    public List<DictColumnBinding> listDictBindings(Long dictId) {
        return dictColumnBindingRepository.findByDictId(dictId);
    }

    @Transactional(readOnly = true)
    public List<DictColumnBinding> listColumnDictBindings(Long metaColumnId) {
        return dictColumnBindingRepository.findByMetaColumnId(metaColumnId);
    }

    @Transactional
    public void deleteDictBinding(Long bindingId) {
        if (!dictColumnBindingRepository.existsById(bindingId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Binding not found: " + bindingId);
        }
        dictColumnBindingRepository.deleteById(bindingId);
    }
}
