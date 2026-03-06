package com.rock.metadata.service;

import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private final CrawlJobRepository crawlJobRepository;
    private final MetaSchemaRepository metaSchemaRepository;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final MetaPrimaryKeyRepository metaPrimaryKeyRepository;
    private final MetaForeignKeyRepository metaForeignKeyRepository;
    private final MetaIndexRepository metaIndexRepository;

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
        job.setStatus(CrawlStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        crawlJobRepository.save(job);

        try {
            Catalog catalog = crawlDatabase(dsConfig, job.getInfoLevel());
            persistCatalog(catalog, dsConfig.getId(), job.getId());

            job.setStatus(CrawlStatus.SUCCESS);
            job.setTableCount(catalog.getTables().size());
            int colCount = catalog.getTables().stream()
                    .mapToInt(t -> t.getColumns().size()).sum();
            job.setColumnCount(colCount);
            log.info("Crawl job {} completed: {} tables, {} columns",
                    job.getId(), job.getTableCount(), colCount);
        } catch (Exception e) {
            log.error("Crawl job {} failed", job.getId(), e);
            job.setStatus(CrawlStatus.FAILED);
            job.setErrorMessage(truncate(e.getMessage(), 4000));
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            crawlJobRepository.save(job);
        }
    }

    private Catalog crawlDatabase(DataSourceConfig ds, String infoLevel) {
        String jdbcUrl = buildJdbcUrl(ds);
        log.info("Crawling database: {}", jdbcUrl);

        DatabaseConnectionSource connectionSource =
                DatabaseConnectionSources.newDatabaseConnectionSource(jdbcUrl,
                        new MultiUseUserCredentials(ds.getUsername(), ds.getPassword()));

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
            limitBuilder.includeSchemas(
                    new RegularExpressionInclusionRule(ds.getSchemaPatterns()));
        }

        SchemaCrawlerOptions options = SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
                .withLimitOptions(limitBuilder.toOptions())
                .withLoadOptions(LoadOptionsBuilder.builder()
                        .withSchemaInfoLevel(schemaInfoLevel).toOptions());

        return SchemaCrawlerUtility.getCatalog(connectionSource, options);
    }

    private void persistCatalog(Catalog catalog, Long datasourceId, Long crawlJobId) {
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
            mt.setSchemaId(schemaIdMap.get(table.getSchema().getFullName()));
            mt.setSchemaName(table.getSchema().getFullName());
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
            mc.setColumnSize(col.getSize());
            mc.setDecimalDigits(col.getDecimalDigits());
            mc.setNullable(col.isNullable());
            mc.setDefaultValue(col.getDefaultValue());
            mc.setAutoIncremented(col.isAutoIncremented());
            mc.setGenerated(col.isGenerated());
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
                indexes.add(mi);
            }
        }
        metaIndexRepository.saveAll(indexes);
    }

    private String buildJdbcUrl(DataSourceConfig ds) {
        if (ds.getJdbcUrl() != null && !ds.getJdbcUrl().isBlank()) {
            return ds.getJdbcUrl();
        }
        String host = ds.getHost() != null ? ds.getHost() : "localhost";
        return switch (ds.getDbType().toLowerCase()) {
            case "postgresql", "postgres" -> {
                int port = ds.getPort() != null ? ds.getPort() : 5432;
                yield "jdbc:postgresql://%s:%d/%s".formatted(host, port, ds.getDatabaseName());
            }
            case "mysql" -> {
                int port = ds.getPort() != null ? ds.getPort() : 3306;
                yield "jdbc:mysql://%s:%d/%s".formatted(host, port, ds.getDatabaseName());
            }
            case "oracle" -> {
                int port = ds.getPort() != null ? ds.getPort() : 1521;
                yield "jdbc:oracle:thin:@%s:%d/%s".formatted(host, port, ds.getDatabaseName());
            }
            case "sqlserver" -> {
                int port = ds.getPort() != null ? ds.getPort() : 1433;
                yield "jdbc:sqlserver://%s:%d;databaseName=%s;trustServerCertificate=true"
                        .formatted(host, port, ds.getDatabaseName());
            }
            case "sqlite" -> "jdbc:sqlite:%s".formatted(ds.getDatabaseName());
            default -> throw new IllegalArgumentException("Unsupported database type: " + ds.getDbType());
        };
    }

    private static String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
