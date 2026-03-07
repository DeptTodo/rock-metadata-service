package com.rock.metadata.controller;

import com.rock.metadata.dto.ColumnQualityCheckResponse;
import com.rock.metadata.dto.ColumnQualityRuleRequest;
import com.rock.metadata.dto.QualityRuleRequest;
import com.rock.metadata.model.ColumnQualityRule;
import com.rock.metadata.model.QualityRule;
import com.rock.metadata.service.DataQualityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
public class DataQualityController {

    private final DataQualityService dataQualityService;

    // ===== 规则定义 CRUD =====

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public QualityRule createRule(@Valid @RequestBody QualityRuleRequest req) {
        return dataQualityService.createRule(req.getRuleCode(), req.getRuleName(),
                req.getRuleType(), req.getDescription(), req.getDefaultSeverity(),
                req.getDefaultParams());
    }

    @GetMapping("/rules")
    public List<QualityRule> listRules(
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Boolean activeOnly) {
        return dataQualityService.listRules(ruleType, activeOnly);
    }

    @GetMapping("/rules/{ruleId}")
    public QualityRule getRule(@PathVariable Long ruleId) {
        return dataQualityService.getRule(ruleId);
    }

    @PutMapping("/rules/{ruleId}")
    public QualityRule updateRule(
            @PathVariable Long ruleId,
            @RequestBody QualityRuleRequest req) {
        return dataQualityService.updateRule(ruleId, req.getRuleName(), req.getDescription(),
                req.getDefaultSeverity(), req.getDefaultParams(), req.getActive());
    }

    @DeleteMapping("/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long ruleId) {
        dataQualityService.deleteRule(ruleId);
    }

    // ===== 字段规则绑定 CRUD =====

    @PostMapping("/column-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ColumnQualityRule bindRuleToColumn(@Valid @RequestBody ColumnQualityRuleRequest req) {
        return dataQualityService.bindRuleToColumn(req.getRuleId(), req.getDatasourceId(),
                req.getSchemaName(), req.getTableName(), req.getColumnName(),
                req.getMetaColumnId(), req.getSeverity(), req.getParams());
    }

    @GetMapping("/column-rules")
    public List<ColumnQualityRule> listColumnRules(
            @RequestParam Long datasourceId,
            @RequestParam(required = false) String schemaName,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String columnName) {
        return dataQualityService.listColumnRules(datasourceId, schemaName, tableName, columnName);
    }

    @GetMapping("/column-rules/by-meta-column/{metaColumnId}")
    public List<ColumnQualityRule> listColumnRulesByMetaColumn(@PathVariable Long metaColumnId) {
        return dataQualityService.listColumnRulesByMetaColumn(metaColumnId);
    }

    @PutMapping("/column-rules/{id}")
    public ColumnQualityRule updateColumnRule(
            @PathVariable Long id,
            @RequestBody ColumnQualityRuleRequest req) {
        return dataQualityService.updateColumnRule(id, req.getSeverity(), req.getParams(), req.getEnabled());
    }

    @DeleteMapping("/column-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteColumnRule(@PathVariable Long id) {
        dataQualityService.deleteColumnRule(id);
    }

    // ===== 质量检查执行 =====

    @PostMapping("/check/column")
    public ColumnQualityCheckResponse checkColumn(
            @RequestParam Long datasourceId,
            @RequestParam(required = false) String schemaName,
            @RequestParam String tableName,
            @RequestParam String columnName) {
        return dataQualityService.executeColumnCheck(datasourceId, schemaName, tableName, columnName);
    }

    @PostMapping("/check/table")
    public List<ColumnQualityCheckResponse> checkTable(
            @RequestParam Long datasourceId,
            @RequestParam(required = false) String schemaName,
            @RequestParam String tableName) {
        return dataQualityService.executeTableCheck(datasourceId, schemaName, tableName);
    }
}
