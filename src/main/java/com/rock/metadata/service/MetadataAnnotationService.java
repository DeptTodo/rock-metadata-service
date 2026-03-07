package com.rock.metadata.service;

import com.rock.metadata.dto.UpdateColumnAttrsRequest;
import com.rock.metadata.dto.UpdateSchemaAttrsRequest;
import com.rock.metadata.dto.UpdateTableAttrsRequest;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.MetaColumnRepository;
import com.rock.metadata.repository.MetaSchemaRepository;
import com.rock.metadata.repository.MetaTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MetadataAnnotationService {

    private final MetaSchemaRepository metaSchemaRepository;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;

    @Transactional
    public MetaSchema updateSchemaAttrs(Long schemaId, UpdateSchemaAttrsRequest req) {
        MetaSchema schema = metaSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Schema not found: " + schemaId));

        if (req.getDisplayName() != null) schema.setDisplayName(req.getDisplayName());
        if (req.getBusinessDescription() != null) schema.setBusinessDescription(req.getBusinessDescription());
        if (req.getOwner() != null) schema.setOwner(req.getOwner());

        return metaSchemaRepository.save(schema);
    }

    @Transactional
    public MetaTable updateTableAttrs(Long tableId, UpdateTableAttrsRequest req) {
        MetaTable table = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

        if (req.getDisplayName() != null) table.setDisplayName(req.getDisplayName());
        if (req.getBusinessDescription() != null) table.setBusinessDescription(req.getBusinessDescription());
        if (req.getBusinessDomain() != null) table.setBusinessDomain(req.getBusinessDomain());
        if (req.getOwner() != null) table.setOwner(req.getOwner());
        if (req.getImportanceLevel() != null) {
            table.setImportanceLevel(ImportanceLevel.valueOf(req.getImportanceLevel().toUpperCase()));
        }
        if (req.getDataQualityScore() != null) table.setDataQualityScore(req.getDataQualityScore());

        return metaTableRepository.save(table);
    }

    @Transactional
    public MetaColumn updateColumnAttrs(Long columnId, UpdateColumnAttrsRequest req) {
        MetaColumn column = metaColumnRepository.findById(columnId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Column not found: " + columnId));

        if (req.getDisplayName() != null) column.setDisplayName(req.getDisplayName());
        if (req.getBusinessDescription() != null) column.setBusinessDescription(req.getBusinessDescription());
        if (req.getBusinessDataType() != null) column.setBusinessDataType(req.getBusinessDataType());
        if (req.getSampleValues() != null) column.setSampleValues(req.getSampleValues());
        if (req.getValueRange() != null) column.setValueRange(req.getValueRange());
        if (req.getSensitivityLevel() != null) {
            column.setSensitivityLevel(SensitivityLevel.valueOf(req.getSensitivityLevel().toUpperCase()));
        }
        if (req.getSensitivityType() != null) column.setSensitivityType(req.getSensitivityType());
        if (req.getMaskingStrategy() != null) column.setMaskingStrategy(req.getMaskingStrategy());
        if (req.getComplianceFlags() != null) column.setComplianceFlags(req.getComplianceFlags());

        return metaColumnRepository.save(column);
    }
}
