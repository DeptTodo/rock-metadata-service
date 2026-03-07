package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.UpdateColumnAttrsRequest;
import com.rock.metadata.dto.UpdateSchemaAttrsRequest;
import com.rock.metadata.dto.UpdateTableAttrsRequest;
import com.rock.metadata.model.MetaColumn;
import com.rock.metadata.model.MetaSchema;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.service.MetadataAnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnotationTools {

    private final MetadataAnnotationService annotationService;

    @Tool(description = "Update business attributes of a schema. " +
            "All fields are optional — only provided fields will be updated. " +
            "Fields: displayName, businessDescription, owner.")
    public MetaSchema update_schema_attrs(
            @ToolParam(description = "Schema ID") Long schemaId,
            @ToolParam(description = "Business display name (optional)", required = false) String displayName,
            @ToolParam(description = "Business description (optional)", required = false) String businessDescription,
            @ToolParam(description = "Data owner / responsible person (optional)", required = false) String owner) {
        return ToolExecutor.run("update schema attrs", () -> {
            UpdateSchemaAttrsRequest req = new UpdateSchemaAttrsRequest();
            req.setDisplayName(displayName);
            req.setBusinessDescription(businessDescription);
            req.setOwner(owner);
            return annotationService.updateSchemaAttrs(schemaId, req);
        });
    }

    @Tool(description = "Update business attributes of a table. " +
            "All fields are optional — only provided fields will be updated. " +
            "Fields: displayName, businessDescription, businessDomain, owner, importanceLevel, dataQualityScore.")
    public MetaTable update_table_attrs(
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "Business display name, e.g. '用户表' (optional)", required = false) String displayName,
            @ToolParam(description = "Business description (optional)", required = false) String businessDescription,
            @ToolParam(description = "Business domain, e.g. '交易域', '用户域' (optional)", required = false) String businessDomain,
            @ToolParam(description = "Data owner / responsible person (optional)", required = false) String owner,
            @ToolParam(description = "Importance level: CORE, IMPORTANT, NORMAL, TRIVIAL (optional)", required = false) String importanceLevel,
            @ToolParam(description = "Data quality score 0-100 (optional)", required = false) Integer dataQualityScore) {
        return ToolExecutor.run("update table attrs", () -> {
            UpdateTableAttrsRequest req = new UpdateTableAttrsRequest();
            req.setDisplayName(displayName);
            req.setBusinessDescription(businessDescription);
            req.setBusinessDomain(businessDomain);
            req.setOwner(owner);
            req.setImportanceLevel(importanceLevel);
            req.setDataQualityScore(dataQualityScore);
            return annotationService.updateTableAttrs(tableId, req);
        });
    }

    @Tool(description = "Update business and security attributes of a column. " +
            "All fields are optional — only provided fields will be updated. " +
            "Fields: displayName, businessDescription, businessDataType, sampleValues, valueRange, " +
            "sensitivityLevel, sensitivityType, maskingStrategy, complianceFlags.")
    public MetaColumn update_column_attrs(
            @ToolParam(description = "Column ID") Long columnId,
            @ToolParam(description = "Business display name, e.g. '年龄' (optional)", required = false) String displayName,
            @ToolParam(description = "Business description (optional)", required = false) String businessDescription,
            @ToolParam(description = "Business data type: AMOUNT, RATE, DATE, CODE, ENUM, ID, NAME, ADDRESS, etc. (optional)", required = false) String businessDataType,
            @ToolParam(description = "Sample values for the column (optional)", required = false) String sampleValues,
            @ToolParam(description = "Value range, e.g. '0-150', 'A/B/C' (optional)", required = false) String valueRange,
            @ToolParam(description = "Sensitivity level: PUBLIC, INTERNAL, SENSITIVE, HIGHLY_SENSITIVE (optional)", required = false) String sensitivityLevel,
            @ToolParam(description = "Sensitivity type: PII, FINANCIAL, MEDICAL, CREDENTIAL, LOCATION, etc. (optional)", required = false) String sensitivityType,
            @ToolParam(description = "Masking strategy: FULL_MASK, PARTIAL_MASK, HASH, ENCRYPT, NONE (optional)", required = false) String maskingStrategy,
            @ToolParam(description = "Compliance flags, comma-separated: GDPR, CCPA, HIPAA, PCI_DSS (optional)", required = false) String complianceFlags) {
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
            return annotationService.updateColumnAttrs(columnId, req);
        });
    }
}
