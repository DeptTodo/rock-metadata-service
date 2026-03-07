package com.rock.metadata.service;

import com.rock.metadata.dto.AdvancedSearchRequest;
import com.rock.metadata.dto.AdvancedSearchResponse;
import com.rock.metadata.dto.RoutineDetailResponse;
import com.rock.metadata.dto.SearchResult;
import com.rock.metadata.dto.TableDetailResponse;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import com.rock.metadata.repository.spec.MetaColumnSpecifications;
import com.rock.metadata.repository.spec.MetaTableSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
    private final MetaRoutineRepository metaRoutineRepository;
    private final MetaRoutineColumnRepository metaRoutineColumnRepository;
    private final MetaSequenceRepository metaSequenceRepository;
    private final LlmAnalysisJobRepository llmAnalysisJobRepository;

    /**
     * Get the latest successful crawl job ID for a datasource.
     */
    public Long getLatestCrawlJobId(Long datasourceId) {
        return crawlJobRepository
                .findFirstByDatasourceIdAndStatusOrderByFinishedAtDesc(
                        datasourceId, CrawlStatus.SUCCESS)
                .map(CrawlJob::getId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

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
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank");
        }
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
            match.setTableFullName(tableNameMap.getOrDefault(col.getTableId(), "unknown"));
            match.setColumn(col);
            columnMatches.add(match);
        }
        result.setColumns(columnMatches);

        return result;
    }

    // ===== Routines =====

    public List<MetaRoutine> listRoutines(Long datasourceId, String schemaName) {
        Long jobId = getLatestCrawlJobId(datasourceId);
        if (schemaName != null && !schemaName.isBlank()) {
            return metaRoutineRepository.findByCrawlJobIdAndSchemaName(jobId, schemaName);
        }
        return metaRoutineRepository.findByCrawlJobId(jobId);
    }

    public RoutineDetailResponse getRoutineDetail(Long routineId) {
        MetaRoutine routine = metaRoutineRepository.findById(routineId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Routine not found: " + routineId));
        RoutineDetailResponse resp = new RoutineDetailResponse();
        resp.setRoutine(routine);
        resp.setColumns(metaRoutineColumnRepository.findByRoutineIdOrderByOrdinalPosition(routineId));
        return resp;
    }

    // ===== Sequences =====

    public List<MetaSequence> listSequences(Long datasourceId, String schemaName) {
        Long jobId = getLatestCrawlJobId(datasourceId);
        if (schemaName != null && !schemaName.isBlank()) {
            return metaSequenceRepository.findByCrawlJobIdAndSchemaName(jobId, schemaName);
        }
        return metaSequenceRepository.findByCrawlJobId(jobId);
    }

    // ===== LLM Analysis Jobs =====

    public List<LlmAnalysisJob> listLlmAnalysisJobs(Long datasourceId) {
        if (datasourceId != null) {
            return llmAnalysisJobRepository.findByDatasourceIdOrderByCreatedAtDesc(datasourceId);
        }
        return llmAnalysisJobRepository.findAll();
    }

    public LlmAnalysisJob getLlmAnalysisJob(Long jobId) {
        return llmAnalysisJobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "LLM analysis job not found: " + jobId));
    }

    // ===== Advanced Search =====

    public AdvancedSearchResponse advancedSearch(Long datasourceId, AdvancedSearchRequest req) {
        Long jobId = getLatestCrawlJobId(datasourceId);

        // Build table specification
        Specification<MetaTable> tableSpec = MetaTableSpecifications.crawlJobIdEquals(jobId);
        if (req.getSchemaName() != null && !req.getSchemaName().isBlank()) {
            tableSpec = tableSpec.and(MetaTableSpecifications.schemaNameEquals(req.getSchemaName()));
        }
        if (req.getTableType() != null && !req.getTableType().isBlank()) {
            tableSpec = tableSpec.and(MetaTableSpecifications.tableTypeEquals(req.getTableType()));
        }
        if (req.getImportanceLevel() != null && !req.getImportanceLevel().isBlank()) {
            tableSpec = tableSpec.and(MetaTableSpecifications.importanceLevelEquals(req.getImportanceLevel()));
        }
        if (req.getBusinessDomain() != null && !req.getBusinessDomain().isBlank()) {
            tableSpec = tableSpec.and(MetaTableSpecifications.businessDomainEquals(req.getBusinessDomain()));
        }
        if (req.getTableNamePattern() != null && !req.getTableNamePattern().isBlank()) {
            tableSpec = tableSpec.and(MetaTableSpecifications.tableNameLike(req.getTableNamePattern()));
        }

        List<MetaTable> tables = metaTableRepository.findAll(tableSpec);

        AdvancedSearchResponse response = new AdvancedSearchResponse();
        response.setTables(tables);
        response.setTableCount(tables.size());

        // Column search if column filters are provided
        boolean hasColumnFilter = req.getDataType() != null || req.getSensitivityLevel() != null
                || req.getNullable() != null || req.getPartOfPrimaryKey() != null
                || req.getPartOfForeignKey() != null || req.getColumnNamePattern() != null;

        if (hasColumnFilter && !tables.isEmpty()) {
            List<Long> tableIds = tables.stream().map(MetaTable::getId).toList();
            Map<Long, String> tableNameMap = tables.stream()
                    .collect(Collectors.toMap(MetaTable::getId, MetaTable::getFullName));

            Specification<MetaColumn> colSpec = MetaColumnSpecifications.tableIdIn(tableIds);
            if (req.getDataType() != null && !req.getDataType().isBlank()) {
                colSpec = colSpec.and(MetaColumnSpecifications.dataTypeEquals(req.getDataType()));
            }
            if (req.getSensitivityLevel() != null && !req.getSensitivityLevel().isBlank()) {
                colSpec = colSpec.and(MetaColumnSpecifications.sensitivityLevelEquals(req.getSensitivityLevel()));
            }
            if (req.getNullable() != null) {
                colSpec = colSpec.and(MetaColumnSpecifications.nullableEquals(req.getNullable()));
            }
            if (req.getPartOfPrimaryKey() != null) {
                colSpec = colSpec.and(MetaColumnSpecifications.partOfPrimaryKey(req.getPartOfPrimaryKey()));
            }
            if (req.getPartOfForeignKey() != null) {
                colSpec = colSpec.and(MetaColumnSpecifications.partOfForeignKey(req.getPartOfForeignKey()));
            }
            if (req.getColumnNamePattern() != null && !req.getColumnNamePattern().isBlank()) {
                colSpec = colSpec.and(MetaColumnSpecifications.columnNameLike(req.getColumnNamePattern()));
            }

            List<MetaColumn> columns = metaColumnRepository.findAll(colSpec);
            List<AdvancedSearchResponse.ColumnResult> colResults = columns.stream()
                    .map(c -> {
                        AdvancedSearchResponse.ColumnResult cr = new AdvancedSearchResponse.ColumnResult();
                        cr.setTableFullName(tableNameMap.getOrDefault(c.getTableId(), "unknown"));
                        cr.setColumn(c);
                        return cr;
                    })
                    .toList();
            response.setColumns(colResults);
            response.setColumnCount(colResults.size());
        } else {
            response.setColumns(List.of());
            response.setColumnCount(0);
        }

        return response;
    }
}
