package com.rock.metadata.service;

import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schema.*;
import schemacrawler.schemacrawler.*;
import schemacrawler.tools.utility.SchemaCrawlerUtility;
import us.fatehi.utility.datasource.DatabaseConnectionSource;
import us.fatehi.utility.datasource.DatabaseConnectionSources;
import us.fatehi.utility.datasource.MultiUseUserCredentials;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private final Set<Long> activeCrawls = ConcurrentHashMap.newKeySet();

    @org.springframework.beans.factory.annotation.Value("${metadata.crawl.retain-count:2}")
    private int retainCount;

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
    private final MetaTagRepository metaTagRepository;
    private final DictColumnBindingRepository dictColumnBindingRepository;
    private final SqlExecuteService sqlExecuteService;

    public CrawlJob createJob(Long datasourceId, String infoLevel) {
        CrawlJob job = new CrawlJob();
        job.setDatasourceId(datasourceId);
        job.setStatus(CrawlStatus.PENDING);
        job.setInfoLevel(infoLevel);
        return crawlJobRepository.save(job);
    }

    @Async
    @Transactional
    public void executeCrawl(CrawlJob job, DataSourceConfig dsConfig) {
        Long datasourceId = dsConfig.getId();
        if (!activeCrawls.add(datasourceId)) {
            job.setStatus(CrawlStatus.FAILED);
            job.setErrorMessage("A crawl is already running for this datasource");
            job.setFinishedAt(LocalDateTime.now());
            crawlJobRepository.save(job);
            return;
        }

        try {
            job.setStatus(CrawlStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            crawlJobRepository.save(job);

            Catalog catalog = crawlDatabase(dsConfig, job.getInfoLevel());
            persistCatalog(catalog, datasourceId, job.getId());

            job.setStatus(CrawlStatus.SUCCESS);
            job.setTableCount(catalog.getTables().size());
            int colCount = catalog.getTables().stream()
                    .mapToInt(t -> t.getColumns().size()).sum();
            job.setColumnCount(colCount);
            job.setRoutineCount(catalog.getRoutines().size());
            job.setSequenceCount(catalog.getSequences().size());
            log.info("Crawl job {} completed: {} tables, {} columns, {} routines, {} sequences",
                    job.getId(), job.getTableCount(), colCount,
                    job.getRoutineCount(), job.getSequenceCount());

            // Auto-count row numbers for all tables after crawl
            try {
                List<MetaTable> tables = metaTableRepository.findByCrawlJobId(job.getId());
                log.info("Starting auto row count for {} tables after crawl job {}", tables.size(), job.getId());
                sqlExecuteService.countTableRows(datasourceId, tables);
                log.info("Auto row count completed for crawl job {}", job.getId());
            } catch (Exception e) {
                log.warn("Auto row count failed for crawl job {} (non-fatal): {}", job.getId(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Crawl job {} failed", job.getId(), e);
            job.setStatus(CrawlStatus.FAILED);
            job.setErrorMessage(truncate(e.getMessage(), 4000));
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            crawlJobRepository.save(job);
            activeCrawls.remove(datasourceId);
        }
    }

    private Catalog crawlDatabase(DataSourceConfig ds, String infoLevel) {
        String jdbcUrl = JdbcUrlBuilder.buildJdbcUrl(ds);
        log.info("Crawling datasource: {} (type={})", ds.getName(), ds.getDbType());

        DatabaseConnectionSource connectionSource =
                DatabaseConnectionSources.newDatabaseConnectionSource(jdbcUrl,
                        new MultiUseUserCredentials(ds.getUsername(), ds.getPassword()));

        try {
            SchemaInfoLevel schemaInfoLevel = switch (infoLevel) {
                case "minimum" -> SchemaInfoLevelBuilder.minimum();
                case "standard" -> SchemaInfoLevelBuilder.standard();
                case "detailed" -> SchemaInfoLevelBuilder.detailed();
                default -> SchemaInfoLevelBuilder.maximum();
            };

            LimitOptionsBuilder limitBuilder = LimitOptionsBuilder.builder()
                    .includeAllRoutines()
                    .includeAllSequences()
                    .includeAllSynonyms();

            if (ds.getSchemaPatterns() != null && !ds.getSchemaPatterns().isBlank()) {
                try {
                    limitBuilder.includeSchemas(
                            new RegularExpressionInclusionRule(ds.getSchemaPatterns()));
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid schema pattern regex: " + ds.getSchemaPatterns());
                }
            }

            SchemaCrawlerOptions options = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
                    .withLimitOptions(limitBuilder.toOptions())
                    .withLoadOptions(LoadOptionsBuilder.builder()
                            .withSchemaInfoLevel(schemaInfoLevel).toOptions());

            return SchemaCrawlerUtility.getCatalog(connectionSource, options);
        } finally {
            try {
                connectionSource.close();
            } catch (Exception e) {
                log.warn("Failed to close connection source", e);
            }
        }
    }

    private void cleanupPreviousCrawlData(Long datasourceId, Long currentCrawlJobId) {
        List<CrawlJob> successJobs = crawlJobRepository
                .findByDatasourceIdAndStatusOrderByFinishedAtDesc(datasourceId, CrawlStatus.SUCCESS);

        // Keep the most recent N successful crawls (excluding current)
        List<CrawlJob> toDelete = successJobs.stream()
                .filter(j -> !j.getId().equals(currentCrawlJobId))
                .skip(retainCount - 1L) // retain (retainCount - 1) old ones + current = retainCount total
                .toList();

        for (CrawlJob oldJob : toDelete) {
            Long prevJobId = oldJob.getId();
            List<MetaTable> oldTables = metaTableRepository.findByCrawlJobId(prevJobId);
            List<Long> oldTableIds = oldTables.stream().map(MetaTable::getId).toList();

            if (!oldTableIds.isEmpty()) {
                metaColumnRepository.deleteByTableIdIn(oldTableIds);
                metaPrimaryKeyRepository.deleteByTableIdIn(oldTableIds);
                metaForeignKeyRepository.deleteByTableIdIn(oldTableIds);
                metaIndexRepository.deleteByTableIdIn(oldTableIds);
                metaTriggerRepository.deleteByTableIdIn(oldTableIds);
                metaConstraintRepository.deleteByTableIdIn(oldTableIds);
                metaPrivilegeRepository.deleteByTableIdIn(oldTableIds);
            }

            List<MetaRoutine> oldRoutines = metaRoutineRepository
                    .findByDatasourceIdAndCrawlJobId(datasourceId, prevJobId);
            List<Long> oldRoutineIds = oldRoutines.stream().map(MetaRoutine::getId).toList();
            if (!oldRoutineIds.isEmpty()) {
                metaRoutineColumnRepository.deleteByRoutineIdIn(oldRoutineIds);
            }

            metaTableRepository.deleteByCrawlJobId(prevJobId);
            metaRoutineRepository.deleteByCrawlJobId(prevJobId);
            metaSequenceRepository.deleteByCrawlJobId(prevJobId);
            metaSchemaRepository.deleteByCrawlJobId(prevJobId);

            log.info("Cleaned up old crawl data for job {}", prevJobId);
        }
    }

    private void persistCatalog(Catalog catalog, Long datasourceId, Long crawlJobId) {
        // Load previous crawl's enriched data before cleanup (may be deleted if retainCount=1)
        Long prevJobId = crawlJobRepository
                .findFirstByDatasourceIdAndStatusOrderByFinishedAtDesc(datasourceId, CrawlStatus.SUCCESS)
                .map(CrawlJob::getId).orElse(null);
        List<MetaSchema> oldSchemas = prevJobId != null
                ? metaSchemaRepository.findByCrawlJobId(prevJobId) : List.of();
        List<MetaTable> oldTables = prevJobId != null
                ? metaTableRepository.findByCrawlJobId(prevJobId) : List.of();
        List<Long> oldTableIds = oldTables.stream().map(MetaTable::getId).toList();
        List<MetaColumn> oldColumns = !oldTableIds.isEmpty()
                ? metaColumnRepository.findByTableIdIn(oldTableIds) : List.of();

        cleanupPreviousCrawlData(datasourceId, crawlJobId);

        // Schema name -> MetaSchema ID
        Map<String, Long> schemaIdMap = new HashMap<>();
        for (Schema schema : catalog.getSchemas()) {
            MetaSchema ms = new MetaSchema();
            ms.setDatasourceId(datasourceId);
            ms.setCrawlJobId(crawlJobId);
            ms.setCatalogName(schema.getCatalogName());
            ms.setSchemaName(schema.getName());
            ms.setFullName(schema.getFullName());
            ms.setRemarks(schema.getRemarks());
            ms = metaSchemaRepository.save(ms);
            schemaIdMap.put(schema.getFullName(), ms.getId());
        }

        for (Table table : catalog.getTables()) {
            MetaTable mt = new MetaTable();
            mt.setDatasourceId(datasourceId);
            mt.setCrawlJobId(crawlJobId);
            String schemaFullName = table.getSchema().getFullName();
            mt.setSchemaId(schemaIdMap.getOrDefault(schemaFullName, null));
            mt.setSchemaName(schemaFullName);
            mt.setTableName(table.getName());
            mt.setFullName(table.getFullName());
            mt.setTableType(table.getTableType().toString());
            mt.setRemarks(table.getRemarks());
            mt.setDefinition(table.getDefinition());
            mt = metaTableRepository.save(mt);
            Long tableId = mt.getId();

            persistColumns(table, tableId);
            persistPrimaryKey(table, tableId);
            persistForeignKeys(table, tableId);
            persistIndexes(table, tableId);
            persistTriggers(table, tableId);
            persistConstraints(table, tableId);
            persistPrivileges(table, tableId);
        }

        persistRoutines(catalog, datasourceId, crawlJobId, schemaIdMap);
        persistSequences(catalog, datasourceId, crawlJobId);

        // Migrate enriched data (business attributes, tags, dict bindings) from previous crawl
        if (prevJobId != null) {
            migrateEnrichedData(crawlJobId, datasourceId, oldSchemas, oldTables, oldColumns);
        }
    }

    private void persistColumns(Table table, Long tableId) {
        List<MetaColumn> columns = new ArrayList<>();
        for (Column col : table.getColumns()) {
            MetaColumn mc = new MetaColumn();
            mc.setTableId(tableId);
            mc.setColumnName(col.getName());
            mc.setOrdinalPosition(col.getOrdinalPosition());
            mc.setDataType(col.getColumnDataType().getName());
            mc.setDbSpecificTypeName(col.getColumnDataType().getDatabaseSpecificTypeName());
            mc.setColumnSize(col.getSize());
            mc.setDecimalDigits(col.getDecimalDigits());
            mc.setNullable(col.isNullable());
            mc.setDefaultValue(col.getDefaultValue());
            mc.setAutoIncremented(col.isAutoIncremented());
            mc.setGenerated(col.isGenerated());
            mc.setHidden(col.isHidden());
            mc.setPartOfPrimaryKey(col.isPartOfPrimaryKey());
            mc.setPartOfForeignKey(col.isPartOfForeignKey());
            mc.setPartOfIndex(col.isPartOfIndex());
            mc.setRemarks(col.getRemarks());
            columns.add(mc);
        }
        metaColumnRepository.saveAll(columns);
    }

    private void persistPrimaryKey(Table table, Long tableId) {
        PrimaryKey pk = table.getPrimaryKey();
        if (pk == null) return;
        List<MetaPrimaryKey> keys = new ArrayList<>();
        for (TableConstraintColumn col : pk.getConstrainedColumns()) {
            MetaPrimaryKey mpk = new MetaPrimaryKey();
            mpk.setTableId(tableId);
            mpk.setConstraintName(pk.getName());
            mpk.setColumnName(col.getName());
            mpk.setKeySequence(col.getTableConstraintOrdinalPosition());
            keys.add(mpk);
        }
        metaPrimaryKeyRepository.saveAll(keys);
    }

    private void persistForeignKeys(Table table, Long tableId) {
        List<MetaForeignKey> fks = new ArrayList<>();
        for (ForeignKey fk : table.getForeignKeys()) {
            for (ColumnReference ref : fk.getColumnReferences()) {
                // Only persist if this table is the FK (child) side
                if (!ref.getForeignKeyColumn().getParent().getFullName()
                        .equals(table.getFullName())) {
                    continue;
                }
                MetaForeignKey mfk = new MetaForeignKey();
                mfk.setTableId(tableId);
                mfk.setFkName(fk.getName());
                mfk.setFkColumnName(ref.getForeignKeyColumn().getName());
                mfk.setPkTableFullName(ref.getPrimaryKeyColumn().getParent().getFullName());
                mfk.setPkColumnName(ref.getPrimaryKeyColumn().getName());
                mfk.setUpdateRule(fk.getUpdateRule().toString());
                mfk.setDeleteRule(fk.getDeleteRule().toString());
                fks.add(mfk);
            }
        }
        metaForeignKeyRepository.saveAll(fks);
    }

    private void persistIndexes(Table table, Long tableId) {
        List<MetaIndex> indexes = new ArrayList<>();
        for (Index index : table.getIndexes()) {
            for (IndexColumn col : index.getColumns()) {
                MetaIndex mi = new MetaIndex();
                mi.setTableId(tableId);
                mi.setIndexName(index.getName());
                mi.setColumnName(col.getName());
                mi.setOrdinalPosition(col.getOrdinalPosition());
                mi.setIndexType(index.getIndexType().toString());
                mi.setUnique(index.isUnique());
                mi.setSortSequence(col.getSortSequence().name());
                mi.setDefinition(index.getDefinition());
                mi.setRemarks(index.getRemarks());
                indexes.add(mi);
            }
        }
        metaIndexRepository.saveAll(indexes);
    }

    private void persistTriggers(Table table, Long tableId) {
        List<MetaTrigger> triggers = new ArrayList<>();
        for (Trigger trigger : table.getTriggers()) {
            MetaTrigger mt = new MetaTrigger();
            mt.setTableId(tableId);
            mt.setTriggerName(trigger.getName());
            mt.setConditionTiming(trigger.getConditionTiming().name());
            mt.setEventManipulationType(
                trigger.getEventManipulationTypes().stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","))
            );
            mt.setActionOrientation(trigger.getActionOrientation().name());
            mt.setActionCondition(trigger.getActionCondition());
            mt.setActionStatement(trigger.getActionStatement());
            mt.setActionOrder(trigger.getActionOrder());
            triggers.add(mt);
        }
        metaTriggerRepository.saveAll(triggers);
    }

    private void persistConstraints(Table table, Long tableId) {
        List<MetaConstraint> constraints = new ArrayList<>();
        for (TableConstraint constraint : table.getTableConstraints()) {
            MetaConstraint mc = new MetaConstraint();
            mc.setTableId(tableId);
            mc.setConstraintName(constraint.getName());
            mc.setConstraintType(constraint.getType().name());
            mc.setColumnNames(
                constraint.getConstrainedColumns().stream()
                    .map(TableConstraintColumn::getName)
                    .collect(Collectors.joining(","))
            );
            mc.setDefinition(constraint.getDefinition());
            mc.setDeferrable(constraint.isDeferrable());
            mc.setInitiallyDeferred(constraint.isInitiallyDeferred());
            constraints.add(mc);
        }
        metaConstraintRepository.saveAll(constraints);
    }

    private void persistPrivileges(Table table, Long tableId) {
        List<MetaPrivilege> privileges = new ArrayList<>();
        for (Privilege<Table> priv : table.getPrivileges()) {
            for (Grant<Table> grant : priv.getGrants()) {
                MetaPrivilege mp = new MetaPrivilege();
                mp.setTableId(tableId);
                mp.setPrivilegeType(priv.getName());
                mp.setGrantor(grant.getGrantor());
                mp.setGrantee(grant.getGrantee());
                mp.setGrantable(grant.isGrantable());
                privileges.add(mp);
            }
        }
        metaPrivilegeRepository.saveAll(privileges);
    }

    private void persistRoutines(Catalog catalog, Long datasourceId, Long crawlJobId,
                                 Map<String, Long> schemaIdMap) {
        for (Routine routine : catalog.getRoutines()) {
            MetaRoutine mr = new MetaRoutine();
            mr.setDatasourceId(datasourceId);
            mr.setCrawlJobId(crawlJobId);
            String routineSchemaFullName = routine.getSchema().getFullName();
            mr.setSchemaId(schemaIdMap.getOrDefault(routineSchemaFullName, null));
            mr.setSchemaName(routineSchemaFullName);
            mr.setRoutineName(routine.getName());
            mr.setFullName(routine.getFullName());
            mr.setSpecificName(routine.getSpecificName());
            mr.setRoutineType(routine.getRoutineType().name());
            mr.setReturnType(routine.getReturnType() != null
                    ? routine.getReturnType().toString() : null);
            mr.setDefinition(routine.getDefinition());
            mr.setRemarks(routine.getRemarks());
            mr = metaRoutineRepository.save(mr);

            Long routineId = mr.getId();
            List<MetaRoutineColumn> cols = new ArrayList<>();
            for (RoutineParameter<?> param : routine.getParameters()) {
                MetaRoutineColumn mrc = new MetaRoutineColumn();
                mrc.setRoutineId(routineId);
                mrc.setColumnName(param.getName());
                mrc.setOrdinalPosition(param.getOrdinalPosition());
                mrc.setColumnType(param.getParameterMode() != null
                        ? param.getParameterMode().name() : null);
                mrc.setDataType(param.getColumnDataType() != null
                        ? param.getColumnDataType().getName() : null);
                mrc.setPrecision(param.getPrecision());
                mrc.setScale(param.getDecimalDigits());
                mrc.setNullable(param.isNullable());
                cols.add(mrc);
            }
            metaRoutineColumnRepository.saveAll(cols);
        }
    }

    private void persistSequences(Catalog catalog, Long datasourceId, Long crawlJobId) {
        List<MetaSequence> sequences = new ArrayList<>();
        for (Sequence seq : catalog.getSequences()) {
            MetaSequence ms = new MetaSequence();
            ms.setDatasourceId(datasourceId);
            ms.setCrawlJobId(crawlJobId);
            ms.setSchemaName(seq.getSchema().getFullName());
            ms.setSequenceName(seq.getName());
            ms.setFullName(seq.getFullName());
            ms.setStartValue(seq.getStartValue());
            ms.setIncrement(seq.getIncrement());
            ms.setMinimumValue(seq.getMinimumValue());
            ms.setMaximumValue(seq.getMaximumValue());
            ms.setCycle(seq.isCycle());
            ms.setRemarks(seq.getRemarks());
            sequences.add(ms);
        }
        metaSequenceRepository.saveAll(sequences);
    }

    // ===== Enriched data migration =====

    private void migrateEnrichedData(Long newCrawlJobId, Long datasourceId,
                                      List<MetaSchema> oldSchemas, List<MetaTable> oldTables,
                                      List<MetaColumn> oldColumns) {
        // Build old data lookup maps
        Map<String, MetaSchema> oldSchemaMap = oldSchemas.stream()
                .collect(Collectors.toMap(MetaSchema::getFullName, s -> s, (a, b) -> a));
        Map<String, MetaTable> oldTableMap = oldTables.stream()
                .collect(Collectors.toMap(MetaTable::getFullName, t -> t, (a, b) -> a));
        Map<Long, String> oldTableIdToFullName = oldTables.stream()
                .collect(Collectors.toMap(MetaTable::getId, MetaTable::getFullName));
        Map<String, MetaColumn> oldColumnMap = oldColumns.stream()
                .collect(Collectors.toMap(
                        c -> oldTableIdToFullName.getOrDefault(c.getTableId(), "") + "\0" + c.getColumnName(),
                        c -> c, (a, b) -> a));

        // Load new data
        List<MetaSchema> newSchemas = metaSchemaRepository.findByCrawlJobId(newCrawlJobId);
        List<MetaTable> newTables = metaTableRepository.findByCrawlJobId(newCrawlJobId);
        List<Long> newTableIds = newTables.stream().map(MetaTable::getId).toList();
        List<MetaColumn> newColumns = !newTableIds.isEmpty()
                ? metaColumnRepository.findByTableIdIn(newTableIds) : List.of();
        Map<Long, String> newTableIdToFullName = newTables.stream()
                .collect(Collectors.toMap(MetaTable::getId, MetaTable::getFullName));

        // ID mappings: old -> new
        Map<Long, Long> schemaIdMapping = new HashMap<>();
        Map<Long, Long> tableIdMapping = new HashMap<>();
        Map<Long, Long> columnIdMapping = new HashMap<>();

        // Migrate schema business attributes
        for (MetaSchema newSchema : newSchemas) {
            MetaSchema oldSchema = oldSchemaMap.get(newSchema.getFullName());
            if (oldSchema != null) {
                schemaIdMapping.put(oldSchema.getId(), newSchema.getId());
                copySchemaAttributes(oldSchema, newSchema);
            }
        }
        if (!schemaIdMapping.isEmpty()) {
            metaSchemaRepository.saveAll(newSchemas);
        }

        // Migrate table business attributes
        for (MetaTable newTable : newTables) {
            MetaTable oldTable = oldTableMap.get(newTable.getFullName());
            if (oldTable != null) {
                tableIdMapping.put(oldTable.getId(), newTable.getId());
                copyTableAttributes(oldTable, newTable);
            }
        }
        if (!tableIdMapping.isEmpty()) {
            metaTableRepository.saveAll(newTables);
        }

        // Migrate column business & security attributes
        for (MetaColumn newCol : newColumns) {
            String key = newTableIdToFullName.getOrDefault(newCol.getTableId(), "") + "\0" + newCol.getColumnName();
            MetaColumn oldCol = oldColumnMap.get(key);
            if (oldCol != null) {
                columnIdMapping.put(oldCol.getId(), newCol.getId());
                copyColumnAttributes(oldCol, newCol);
            }
        }
        if (!columnIdMapping.isEmpty()) {
            metaColumnRepository.saveAll(newColumns);
        }

        // Update tag references to point to new IDs
        updateTagReferences("SCHEMA", schemaIdMapping);
        updateTagReferences("TABLE", tableIdMapping);
        updateTagReferences("COLUMN", columnIdMapping);

        // Update dict binding references to point to new column IDs
        updateDictBindingReferences(columnIdMapping);

        log.info("Migrated enriched data: {} schemas, {} tables, {} columns",
                schemaIdMapping.size(), tableIdMapping.size(), columnIdMapping.size());
    }

    private void copySchemaAttributes(MetaSchema from, MetaSchema to) {
        to.setDisplayName(from.getDisplayName());
        to.setBusinessDescription(from.getBusinessDescription());
        to.setOwner(from.getOwner());
    }

    private void copyTableAttributes(MetaTable from, MetaTable to) {
        to.setDisplayName(from.getDisplayName());
        to.setBusinessDescription(from.getBusinessDescription());
        to.setBusinessDomain(from.getBusinessDomain());
        to.setOwner(from.getOwner());
        to.setImportanceLevel(from.getImportanceLevel());
        to.setDataQualityScore(from.getDataQualityScore());
        to.setRowCount(from.getRowCount());
        to.setLastAnalyzedAt(from.getLastAnalyzedAt());
        to.setAnalysisConfidence(from.getAnalysisConfidence());
    }

    private void copyColumnAttributes(MetaColumn from, MetaColumn to) {
        to.setDisplayName(from.getDisplayName());
        to.setBusinessDescription(from.getBusinessDescription());
        to.setBusinessDataType(from.getBusinessDataType());
        to.setSampleValues(from.getSampleValues());
        to.setValueRange(from.getValueRange());
        to.setSensitivityLevel(from.getSensitivityLevel());
        to.setSensitivityType(from.getSensitivityType());
        to.setMaskingStrategy(from.getMaskingStrategy());
        to.setComplianceFlags(from.getComplianceFlags());
        to.setLastAnalyzedAt(from.getLastAnalyzedAt());
        to.setAnalysisConfidence(from.getAnalysisConfidence());
    }

    private void updateTagReferences(String targetType, Map<Long, Long> idMapping) {
        if (idMapping.isEmpty()) return;
        List<MetaTag> tags = metaTagRepository.findByTargetTypeAndTargetIdIn(
                targetType, new ArrayList<>(idMapping.keySet()));
        if (tags.isEmpty()) return;
        for (MetaTag tag : tags) {
            Long newId = idMapping.get(tag.getTargetId());
            if (newId != null) {
                tag.setTargetId(newId);
            }
        }
        metaTagRepository.saveAll(tags);
    }

    private void updateDictBindingReferences(Map<Long, Long> columnIdMapping) {
        if (columnIdMapping.isEmpty()) return;
        List<DictColumnBinding> bindings = dictColumnBindingRepository.findByMetaColumnIdIn(
                new ArrayList<>(columnIdMapping.keySet()));
        if (bindings.isEmpty()) return;
        for (DictColumnBinding binding : bindings) {
            Long newId = columnIdMapping.get(binding.getMetaColumnId());
            if (newId != null) {
                binding.setMetaColumnId(newId);
            }
        }
        dictColumnBindingRepository.saveAll(bindings);
    }

    private static String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
