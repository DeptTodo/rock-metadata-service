package com.rock.metadata.service;

import com.rock.metadata.dto.SearchResult;
import com.rock.metadata.dto.TableDetailResponse;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetadataQueryService {

    private final CrawlJobRepository crawlJobRepository;
    private final MetaSchemaRepository metaSchemaRepository;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final MetaPrimaryKeyRepository metaPrimaryKeyRepository;
    private final MetaForeignKeyRepository metaForeignKeyRepository;
    private final MetaIndexRepository metaIndexRepository;
    private final MetaTriggerRepository metaTriggerRepository;
    private final MetaConstraintRepository metaConstraintRepository;
    private final MetaPrivilegeRepository metaPrivilegeRepository;

    /**
     * Get the latest successful crawl job ID for a datasource.
     */
    public Long getLatestCrawlJobId(Long datasourceId) {
        return crawlJobRepository
                .findFirstByDatasourceIdAndStatusOrderByFinishedAtDesc(
                        datasourceId, CrawlStatus.SUCCESS)
                .map(CrawlJob::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "No successful crawl found for datasource " + datasourceId));
    }

    public List<MetaSchema> listSchemas(Long datasourceId) {
        Long jobId = getLatestCrawlJobId(datasourceId);
        return metaSchemaRepository.findByCrawlJobId(jobId);
    }

    public List<MetaTable> listTables(Long datasourceId, String schemaName) {
        Long jobId = getLatestCrawlJobId(datasourceId);
        if (schemaName != null && !schemaName.isBlank()) {
            return metaTableRepository.findByCrawlJobIdAndSchemaName(jobId, schemaName);
        }
        return metaTableRepository.findByCrawlJobId(jobId);
    }

    public TableDetailResponse getTableDetail(Long tableId) {
        MetaTable table = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableId));

        TableDetailResponse resp = new TableDetailResponse();
        resp.setTable(table);
        resp.setColumns(metaColumnRepository.findByTableIdOrderByOrdinalPosition(tableId));
        resp.setPrimaryKeys(metaPrimaryKeyRepository.findByTableId(tableId));
        resp.setForeignKeys(metaForeignKeyRepository.findByTableId(tableId));
        resp.setIndexes(metaIndexRepository.findByTableId(tableId));
        resp.setTriggers(metaTriggerRepository.findByTableId(tableId));
        resp.setConstraints(metaConstraintRepository.findByTableId(tableId));
        resp.setPrivileges(metaPrivilegeRepository.findByTableId(tableId));
        return resp;
    }

    public List<MetaColumn> listColumns(Long tableId) {
        return metaColumnRepository.findByTableIdOrderByOrdinalPosition(tableId);
    }

    public List<MetaForeignKey> listForeignKeys(Long tableId) {
        return metaForeignKeyRepository.findByTableId(tableId);
    }

    public List<MetaIndex> listIndexes(Long tableId) {
        return metaIndexRepository.findByTableId(tableId);
    }

    public SearchResult search(Long datasourceId, String keyword) {
        Long jobId = getLatestCrawlJobId(datasourceId);
        SearchResult result = new SearchResult();

        // Search tables
        List<MetaTable> tables = metaTableRepository.searchByKeyword(jobId, keyword);
        result.setTables(tables);

        // Search columns across all tables of this crawl job
        List<MetaTable> allTables = metaTableRepository.findByCrawlJobId(jobId);
        List<Long> tableIds = allTables.stream().map(MetaTable::getId).toList();

        Map<Long, String> tableNameMap = allTables.stream()
                .collect(Collectors.toMap(MetaTable::getId, MetaTable::getFullName));

        List<MetaColumn> matchedColumns = metaColumnRepository.searchByKeyword(tableIds, keyword);
        List<SearchResult.ColumnMatch> columnMatches = new ArrayList<>();
        for (MetaColumn col : matchedColumns) {
            SearchResult.ColumnMatch match = new SearchResult.ColumnMatch();
            match.setTableFullName(tableNameMap.get(col.getTableId()));
            match.setColumn(col);
            columnMatches.add(match);
        }
        result.setColumns(columnMatches);

        return result;
    }
}
