package com.rock.metadata.service;

import com.rock.metadata.dto.DatasourceSummary;
import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatasourceSummaryService {

    private final MetadataQueryService metadataQueryService;
    private final MetaSchemaRepository metaSchemaRepository;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final MetaIndexRepository metaIndexRepository;
    private final MetaRoutineRepository metaRoutineRepository;
    private final MetaSequenceRepository metaSequenceRepository;
    private final CrawlJobRepository crawlJobRepository;

    public DatasourceSummary getSummary(Long datasourceId) {
        Long jobId = metadataQueryService.getLatestCrawlJobId(datasourceId);
        DatasourceSummary summary = new DatasourceSummary();
        summary.setDatasourceId(datasourceId);

        List<MetaSchema> schemas = metaSchemaRepository.findByCrawlJobId(jobId);
        List<MetaTable> tables = metaTableRepository.findByCrawlJobId(jobId);
        List<Long> tableIds = tables.stream().map(MetaTable::getId).toList();

        summary.setSchemaCount(schemas.size());
        summary.setTableCount(tables.size());

        // Column count and type distribution
        List<MetaColumn> allColumns = new ArrayList<>();
        for (Long tableId : tableIds) {
            allColumns.addAll(metaColumnRepository.findByTableIdOrderByOrdinalPosition(tableId));
        }
        summary.setColumnCount(allColumns.size());

        // Routines and sequences
        List<MetaRoutine> routines = metaRoutineRepository.findByCrawlJobId(jobId);
        List<MetaSequence> sequences = metaSequenceRepository.findByCrawlJobId(jobId);
        summary.setRoutineCount(routines.size());
        summary.setSequenceCount(sequences.size());

        // Table type distribution
        Map<String, Integer> tableTypeDist = tables.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTableType() != null ? t.getTableType() : "UNKNOWN",
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
        summary.setTableTypeDistribution(tableTypeDist);

        // Column type top N
        Map<String, Long> colTypeCounts = allColumns.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getDataType() != null ? c.getDataType() : "UNKNOWN",
                        Collectors.counting()));
        List<DatasourceSummary.TypeCount> colTypeTop = colTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    DatasourceSummary.TypeCount tc = new DatasourceSummary.TypeCount();
                    tc.setType(e.getKey());
                    tc.setCount(e.getValue().intValue());
                    return tc;
                })
                .toList();
        summary.setColumnTypeTop(colTypeTop);

        // Tables with most columns (top 10)
        Map<Long, Long> colCountByTable = allColumns.stream()
                .collect(Collectors.groupingBy(MetaColumn::getTableId, Collectors.counting()));
        Map<Long, MetaTable> tableMap = tables.stream()
                .collect(Collectors.toMap(MetaTable::getId, t -> t));
        List<DatasourceSummary.TableStat> mostCols = colCountByTable.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    DatasourceSummary.TableStat stat = new DatasourceSummary.TableStat();
                    MetaTable t = tableMap.get(e.getKey());
                    if (t != null) {
                        stat.setTableId(t.getId());
                        stat.setTableName(t.getTableName());
                        stat.setFullName(t.getFullName());
                    }
                    stat.setCount(e.getValue().intValue());
                    return stat;
                })
                .toList();
        summary.setTablesWithMostColumns(mostCols);

        // Tables with most indexes (top 10)
        Map<Long, Long> idxCountByTable = new HashMap<>();
        for (Long tableId : tableIds) {
            List<MetaIndex> indexes = metaIndexRepository.findByTableId(tableId);
            long distinctIndexes = indexes.stream()
                    .map(MetaIndex::getIndexName)
                    .distinct()
                    .count();
            if (distinctIndexes > 0) {
                idxCountByTable.put(tableId, distinctIndexes);
            }
        }
        List<DatasourceSummary.TableStat> mostIdx = idxCountByTable.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    DatasourceSummary.TableStat stat = new DatasourceSummary.TableStat();
                    MetaTable t = tableMap.get(e.getKey());
                    if (t != null) {
                        stat.setTableId(t.getId());
                        stat.setTableName(t.getTableName());
                        stat.setFullName(t.getFullName());
                    }
                    stat.setCount(e.getValue().intValue());
                    return stat;
                })
                .toList();
        summary.setTablesWithMostIndexes(mostIdx);

        // Last crawl info
        crawlJobRepository.findFirstByDatasourceIdAndStatusOrderByFinishedAtDesc(
                datasourceId, CrawlStatus.SUCCESS)
                .ifPresent(job -> {
                    summary.setLastCrawlTime(job.getFinishedAt());
                    if (job.getStartedAt() != null && job.getFinishedAt() != null) {
                        summary.setLastCrawlDurationMs(
                                Duration.between(job.getStartedAt(), job.getFinishedAt()).toMillis());
                    }
                });

        return summary;
    }
}
