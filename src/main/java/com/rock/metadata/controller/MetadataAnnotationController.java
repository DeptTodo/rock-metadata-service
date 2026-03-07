package com.rock.metadata.controller;

import com.rock.metadata.dto.UpdateColumnAttrsRequest;
import com.rock.metadata.dto.UpdateSchemaAttrsRequest;
import com.rock.metadata.dto.UpdateTableAttrsRequest;
import com.rock.metadata.model.MetaColumn;
import com.rock.metadata.model.MetaSchema;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.service.MetadataAnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetadataAnnotationController {

    private final MetadataAnnotationService annotationService;

    @PatchMapping("/schemas/{schemaId}/attrs")
    public MetaSchema updateSchemaAttrs(
            @PathVariable Long schemaId,
            @RequestBody UpdateSchemaAttrsRequest request) {
        return annotationService.updateSchemaAttrs(schemaId, request);
    }

    @PatchMapping("/tables/{tableId}/attrs")
    public MetaTable updateTableAttrs(
            @PathVariable Long tableId,
            @RequestBody UpdateTableAttrsRequest request) {
        return annotationService.updateTableAttrs(tableId, request);
    }

    @PatchMapping("/columns/{columnId}/attrs")
    public MetaColumn updateColumnAttrs(
            @PathVariable Long columnId,
            @RequestBody UpdateColumnAttrsRequest request) {
        return annotationService.updateColumnAttrs(columnId, request);
    }
}
