package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.UpdateColumnAttrsRequest;
import com.rock.metadata.dto.UpdateSchemaAttrsRequest;
import com.rock.metadata.dto.UpdateTableAttrsRequest;
import com.rock.metadata.service.MetadataAnnotationService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AnnotationTools {

    private final MetadataAnnotationService annotationService;

    @McpTool(description = "Update business attributes of a schema. " +
            "All fields are optional — only provided fields will be updated. " +
            "Fields: displayName, businessDescription, owner.")
    public Map<String, Object> update_schema_attrs(
            @McpToolParam(description = "Schema ID") Long schemaId,
            @McpToolParam(description = "Business display name (optional)", required = false) String displayName,
            @McpToolParam(description = "Business description (optional)", required = false) String businessDescription,
            @McpToolParam(description = "Data owner / responsible person (optional)", required = false) String owner) {
        return ToolExecutor.run("update schema attrs", () -> {
            UpdateSchemaAttrsRequest req = new UpdateSchemaAttrsRequest();
            req.setDisplayName(displayName);
            req.setBusinessDescription(businessDescription);
            req.setOwner(owner);
            return McpResponseHelper.compact(annotationService.updateSchemaAttrs(schemaId, req));
        });
    }

    @McpTool(description = "Update business attributes of a table. " +
            "All fields are optional — only provided fields will be updated. " +
            "Fields: displayName, businessDescription, businessDomain, owner, importanceLevel, dataQualityScore.")
    public Map<String, Object> update_table_attrs(
            @McpToolParam(description = "Table ID") Long tableId,
            @McpToolParam(description = "Business display name, e.g. '用户表' (optional)", required = false) String displayName,
            @McpToolParam(description = "Business description (optional)", required = false) String businessDescription,
            @McpToolParam(description = "Business domain, e.g. '交易域', '用户域' (optional)", required = false) String businessDomain,
            @McpToolParam(description = "Data owner / responsible person (optional)", required = false) String owner,
            @McpToolParam(description = "Importance level: CORE, IMPORTANT, NORMAL, TRIVIAL (optional)", required = false) String importanceLevel,
            @McpToolParam(description = "Data quality score 0-100 (optional)", required = false) Integer dataQualityScore) {
        return ToolExecutor.run("update table attrs", () -> {
            UpdateTableAttrsRequest req = new UpdateTableAttrsRequest();
            req.setDisplayName(displayName);
            req.setBusinessDescription(businessDescription);
            req.setBusinessDomain(businessDomain);
            req.setOwner(owner);
            req.setImportanceLevel(importanceLevel);
            req.setDataQualityScore(dataQualityScore);
            return McpResponseHelper.compact(annotationService.updateTableAttrs(tableId, req));
        });
    }

    @McpTool(description = "Update business and security attributes of a column. " +
            "All fields are optional — only provided fields will be updated. " +
            "Fields: displayName, businessDescription, businessDataType, sampleValues, valueRange, " +
            "sensitivityLevel, sensitivityType, maskingStrategy, complianceFlags.")
    public Map<String, Object> update_column_attrs(
            @McpToolParam(description = "Column ID") Long columnId,
            @McpToolParam(description = "Business display name, e.g. '年龄' (optional)", required = false) String displayName,
            @McpToolParam(description = "Business description (optional)", required = false) String businessDescription,
            @McpToolParam(description = "Business data type: AMOUNT, RATE, DATE, CODE, ENUM, ID, NAME, ADDRESS, etc. (optional)", required = false) String businessDataType,
            @McpToolParam(description = "Sample values for the column (optional)", required = false) String sampleValues,
            @McpToolParam(description = "Value range, e.g. '0-150', 'A/B/C' (optional)", required = false) String valueRange,
            @McpToolParam(description = "Sensitivity level: PUBLIC, INTERNAL, SENSITIVE, HIGHLY_SENSITIVE (optional)", required = false) String sensitivityLevel,
            @McpToolParam(description = "Sensitivity type: PII, FINANCIAL, MEDICAL, CREDENTIAL, LOCATION, etc. (optional)", required = false) String sensitivityType,
            @McpToolParam(description = "Masking strategy: FULL_MASK, PARTIAL_MASK, HASH, ENCRYPT, NONE (optional)", required = false) String maskingStrategy,
            @McpToolParam(description = "Compliance flags, comma-separated: GDPR, CCPA, HIPAA, PCI_DSS (optional)", required = false) String complianceFlags) {
        return ToolExecutor.run("update column attrs", () -> {
            UpdateColumnAttrsRequest req = new UpdateColumnAttrsRequest();
            req.setDisplayName(displayName);
            req.setBusinessDescription(businessDescription);
            req.setBusinessDataType(businessDataType);
            req.setSampleValues(sampleValues);
            req.setValueRange(valueRange);
            req.setSensitivityLevel(sensitivityLevel);
            req.setSensitivityType(sensitivityType);
            req.setMaskingStrategy(maskingStrategy);
            req.setComplianceFlags(complianceFlags);
            return McpResponseHelper.compact(annotationService.updateColumnAttrs(columnId, req));
        });
    }
}
