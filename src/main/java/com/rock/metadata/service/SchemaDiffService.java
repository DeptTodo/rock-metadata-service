package com.rock.metadata.service;

import com.rock.metadata.dto.SchemaDiffResponse;
import com.rock.metadata.dto.SchemaDiffResponse.*;
import com.rock.metadata.model.CrawlJob;
import com.rock.metadata.model.CrawlStatus;
import com.rock.metadata.model.MetaColumn;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.repository.CrawlJobRepository;
import com.rock.metadata.repository.MetaColumnRepository;
import com.rock.metadata.repository.MetaTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchemaDiffService {

    private final CrawlJobRepository crawlJobRepository;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;

    public SchemaDiffResponse compareCrawls(Long datasourceId, Long crawlJobId1, Long crawlJobId2) {
        if (crawlJobId1 == null || crawlJobId2 == null) {
            // Default: compare last two successful crawls
            List<CrawlJob> jobs = crawlJobRepository
                    .findByDatasourceIdAndStatusOrderByFinishedAtDesc(datasourceId, CrawlStatus.SUCCESS);
            if (jobs.size() < 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Need at least 2 successful crawls to compare. Found: " + jobs.size());
            }
            crawlJobId1 = jobs.get(1).getId(); // older
            crawlJobId2 = jobs.get(0).getId(); // newer
        }

        List<MetaTable> tables1 = metaTableRepository.findByCrawlJobId(crawlJobId1);
        List<MetaTable> tables2 = metaTableRepository.findByCrawlJobId(crawlJobId2);

        Map<String, MetaTable> tableMap1 = tables1.stream()
                .collect(Collectors.toMap(MetaTable::getFullName, t -> t));
        Map<String, MetaTable> tableMap2 = tables2.stream()
                .collect(Collectors.toMap(MetaTable::getFullName, t -> t));

        SchemaDiffResponse response = new SchemaDiffResponse();
        response.setCrawlJobId1(crawlJobId1);
        response.setCrawlJobId2(crawlJobId2);

        // Added tables (in new but not in old)
        List<TableDiff> added = new ArrayList<>();
        for (MetaTable t : tables2) {
            if (!tableMap1.containsKey(t.getFullName())) {
                added.add(toTableDiff(t));
            }
        }
        response.setAddedTables(added);

        // Removed tables (in old but not in new)
        List<TableDiff> removed = new ArrayList<>();
        for (MetaTable t : tables1) {
            if (!tableMap2.containsKey(t.getFullName())) {
                removed.add(toTableDiff(t));
            }
        }
        response.setRemovedTables(removed);

        // Modified tables (in both)
        List<TableModification> modified = new ArrayList<>();
        for (MetaTable t1 : tables1) {
            MetaTable t2 = tableMap2.get(t1.getFullName());
            if (t2 == null) continue;

            TableModification mod = compareTable(t1, t2);
            if (mod != null) {
                modified.add(mod);
            }
        }
        response.setModifiedTables(modified);

        // Summary
        DiffSummary summary = new DiffSummary();
        summary.setAddedTableCount(added.size());
        summary.setRemovedTableCount(removed.size());
        summary.setModifiedTableCount(modified.size());
        summary.setTotalAddedColumns(modified.stream()
                .mapToInt(m -> m.getAddedColumns().size()).sum());
        summary.setTotalRemovedColumns(modified.stream()
                .mapToInt(m -> m.getRemovedColumns().size()).sum());
        summary.setTotalModifiedColumns(modified.stream()
                .mapToInt(m -> m.getModifiedColumns().size()).sum());
        response.setSummary(summary);

        return response;
    }

    private TableModification compareTable(MetaTable t1, MetaTable t2) {
        List<MetaColumn> cols1 = metaColumnRepository.findByTableIdOrderByOrdinalPosition(t1.getId());
        List<MetaColumn> cols2 = metaColumnRepository.findByTableIdOrderByOrdinalPosition(t2.getId());

        Map<String, MetaColumn> colMap1 = cols1.stream()
                .collect(Collectors.toMap(MetaColumn::getColumnName, c -> c));
        Map<String, MetaColumn> colMap2 = cols2.stream()
                .collect(Collectors.toMap(MetaColumn::getColumnName, c -> c));

        List<ColumnDiff> addedCols = new ArrayList<>();
        for (MetaColumn c : cols2) {
            if (!colMap1.containsKey(c.getColumnName())) {
                addedCols.add(toColumnDiff(c));
            }
        }

        List<ColumnDiff> removedCols = new ArrayList<>();
        for (MetaColumn c : cols1) {
            if (!colMap2.containsKey(c.getColumnName())) {
                removedCols.add(toColumnDiff(c));
            }
        }

        List<ColumnModification> modifiedCols = new ArrayList<>();
        for (MetaColumn c1 : cols1) {
            MetaColumn c2 = colMap2.get(c1.getColumnName());
            if (c2 == null) continue;

            List<PropertyChange> changes = compareColumn(c1, c2);
            if (!changes.isEmpty()) {
                ColumnModification cm = new ColumnModification();
                cm.setColumnName(c1.getColumnName());
                cm.setChanges(changes);
                modifiedCols.add(cm);
            }
        }

        if (addedCols.isEmpty() && removedCols.isEmpty() && modifiedCols.isEmpty()) {
            return null;
        }

        TableModification mod = new TableModification();
        mod.setFullName(t1.getFullName());
        mod.setTableName(t1.getTableName());
        mod.setAddedColumns(addedCols);
        mod.setRemovedColumns(removedCols);
        mod.setModifiedColumns(modifiedCols);
        return mod;
    }

    private List<PropertyChange> compareColumn(MetaColumn c1, MetaColumn c2) {
        List<PropertyChange> changes = new ArrayList<>();

        compareProperty(changes, "dataType", c1.getDataType(), c2.getDataType());
        compareProperty(changes, "nullable", String.valueOf(c1.isNullable()), String.valueOf(c2.isNullable()));
        compareProperty(changes, "columnSize", String.valueOf(c1.getColumnSize()), String.valueOf(c2.getColumnSize()));
        compareProperty(changes, "decimalDigits", String.valueOf(c1.getDecimalDigits()), String.valueOf(c2.getDecimalDigits()));
        compareProperty(changes, "defaultValue", c1.getDefaultValue(), c2.getDefaultValue());
        compareProperty(changes, "autoIncremented", String.valueOf(c1.isAutoIncremented()), String.valueOf(c2.isAutoIncremented()));

        return changes;
    }

    private void compareProperty(List<PropertyChange> changes, String property, String old, String _new) {
        String oldVal = old != null ? old : "";
        String newVal = _new != null ? _new : "";
        if (!oldVal.equals(newVal)) {
            PropertyChange change = new PropertyChange();
            change.setProperty(property);
            change.setOldValue(oldVal);
            change.setNewValue(newVal);
            changes.add(change);
        }
    }

    private TableDiff toTableDiff(MetaTable t) {
        TableDiff diff = new TableDiff();
        diff.setFullName(t.getFullName());
        diff.setTableName(t.getTableName());
        diff.setSchemaName(t.getSchemaName());
        diff.setTableType(t.getTableType());
        return diff;
    }

    private ColumnDiff toColumnDiff(MetaColumn c) {
        ColumnDiff diff = new ColumnDiff();
        diff.setColumnName(c.getColumnName());
        diff.setDataType(c.getDataType());
        diff.setNullable(c.isNullable());
        return diff;
    }
}
