package com.rock.metadata.controller;

import com.rock.metadata.dto.SearchResult;
import com.rock.metadata.dto.TableDetailResponse;
import com.rock.metadata.model.*;
import com.rock.metadata.service.MetadataQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataQueryService queryService;

    /** List schemas for a datasource (latest successful crawl). */
    @GetMapping("/datasources/{datasourceId}/schemas")
    public List<MetaSchema> listSchemas(@PathVariable Long datasourceId) {
        return queryService.listSchemas(datasourceId);
    }

    /** List tables for a datasource, optionally filter by schema. */
    @GetMapping("/datasources/{datasourceId}/tables")
    public List<MetaTable> listTables(
            @PathVariable Long datasourceId,
            @RequestParam(required = false) String schema) {
        return queryService.listTables(datasourceId, schema);
    }

    /** Get full detail of a table: columns, primary keys, foreign keys, indexes. */
    @GetMapping("/tables/{tableId}")
    public TableDetailResponse getTableDetail(@PathVariable Long tableId) {
        return queryService.getTableDetail(tableId);
    }

    @GetMapping("/tables/{tableId}/columns")
    public List<MetaColumn> listColumns(@PathVariable Long tableId) {
        return queryService.listColumns(tableId);
    }

    @GetMapping("/tables/{tableId}/foreign-keys")
    public List<MetaForeignKey> listForeignKeys(@PathVariable Long tableId) {
        return queryService.listForeignKeys(tableId);
    }

    @GetMapping("/tables/{tableId}/indexes")
    public List<MetaIndex> listIndexes(@PathVariable Long tableId) {
        return queryService.listIndexes(tableId);
    }

    /** Search tables and columns by keyword across a datasource. */
    @GetMapping("/datasources/{datasourceId}/search")
    public SearchResult search(
            @PathVariable Long datasourceId,
            @RequestParam String keyword) {
        return queryService.search(datasourceId, keyword);
    }
}
