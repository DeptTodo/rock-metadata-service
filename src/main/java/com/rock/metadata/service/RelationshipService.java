package com.rock.metadata.service;

import com.rock.metadata.dto.ImpactAnalysisResponse;
import com.rock.metadata.dto.ImpactAnalysisResponse.AffectedTable;
import com.rock.metadata.dto.TableRelationshipResponse;
import com.rock.metadata.dto.TableRelationshipResponse.RelationshipEdge;
import com.rock.metadata.dto.TableRelationshipResponse.TableNode;
import com.rock.metadata.model.MetaForeignKey;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.repository.MetaForeignKeyRepository;
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
public class RelationshipService {

    private final MetaTableRepository metaTableRepository;
    private final MetaForeignKeyRepository metaForeignKeyRepository;

    public TableRelationshipResponse getTableRelationships(Long tableId, Integer depth) {
        int maxDepth = depth != null ? Math.min(depth, 5) : 1;

        MetaTable rootTable = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

        // Load all tables and FKs for this crawl job
        List<MetaTable> allTables = metaTableRepository.findByCrawlJobId(rootTable.getCrawlJobId());
        Map<String, MetaTable> tableByFullName = allTables.stream()
                .collect(Collectors.toMap(MetaTable::getFullName, t -> t, (a, b) -> a));
        Map<Long, List<MetaForeignKey>> fksByTableId = loadForeignKeys(allTables);

        // Build outgoing (this table references) and incoming (references this table) maps
        Map<String, List<MetaForeignKey>> outgoing = new HashMap<>();
        Map<String, List<MetaForeignKey>> incoming = new HashMap<>();

        for (MetaTable t : allTables) {
            List<MetaForeignKey> fks = fksByTableId.getOrDefault(t.getId(), List.of());
            for (MetaForeignKey fk : fks) {
                outgoing.computeIfAbsent(t.getFullName(), k -> new ArrayList<>()).add(fk);
                incoming.computeIfAbsent(fk.getPkTableFullName(), k -> new ArrayList<>()).add(fk);
            }
        }

        // BFS traversal both directions
        List<RelationshipEdge> edges = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<Map.Entry<String, Integer>> queue = new LinkedList<>();
        queue.add(Map.entry(rootTable.getFullName(), 0));
        visited.add(rootTable.getFullName());

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> entry = queue.poll();
            String currentTable = entry.getKey();
            int currentDepth = entry.getValue();

            if (currentDepth >= maxDepth) continue;

            // Outgoing (this table references others via FK)
            for (MetaForeignKey fk : outgoing.getOrDefault(currentTable, List.of())) {
                RelationshipEdge edge = new RelationshipEdge();
                edge.setFromTable(currentTable);
                edge.setToTable(fk.getPkTableFullName());
                edge.setFkName(fk.getFkName());
                edge.setFromColumn(fk.getFkColumnName());
                edge.setToColumn(fk.getPkColumnName());
                edge.setDirection("OUTGOING");
                edge.setDepth(currentDepth + 1);
                edges.add(edge);

                if (!visited.contains(fk.getPkTableFullName())) {
                    visited.add(fk.getPkTableFullName());
                    queue.add(Map.entry(fk.getPkTableFullName(), currentDepth + 1));
                }
            }

            // Incoming (others reference this table via FK)
            for (MetaForeignKey fk : incoming.getOrDefault(currentTable, List.of())) {
                MetaTable fkTable = allTables.stream()
                        .filter(t -> t.getId().equals(fk.getTableId()))
                        .findFirst().orElse(null);
                if (fkTable == null) continue;

                RelationshipEdge edge = new RelationshipEdge();
                edge.setFromTable(fkTable.getFullName());
                edge.setToTable(currentTable);
                edge.setFkName(fk.getFkName());
                edge.setFromColumn(fk.getFkColumnName());
                edge.setToColumn(fk.getPkColumnName());
                edge.setDirection("INCOMING");
                edge.setDepth(currentDepth + 1);
                edges.add(edge);

                if (!visited.contains(fkTable.getFullName())) {
                    visited.add(fkTable.getFullName());
                    queue.add(Map.entry(fkTable.getFullName(), currentDepth + 1));
                }
            }
        }

        TableRelationshipResponse response = new TableRelationshipResponse();
        TableNode root = new TableNode();
        root.setTableId(rootTable.getId());
        root.setTableName(rootTable.getTableName());
        root.setFullName(rootTable.getFullName());
        root.setSchemaName(rootTable.getSchemaName());
        response.setRootTable(root);
        response.setEdges(edges);
        response.setDepth(maxDepth);
        return response;
    }

    public ImpactAnalysisResponse getImpactAnalysis(Long tableId) {
        MetaTable rootTable = metaTableRepository.findById(tableId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Table not found: " + tableId));

        List<MetaTable> allTables = metaTableRepository.findByCrawlJobId(rootTable.getCrawlJobId());
        Map<Long, MetaTable> tableById = allTables.stream()
                .collect(Collectors.toMap(MetaTable::getId, t -> t, (a, b) -> a));
        Map<Long, List<MetaForeignKey>> fksByTableId = loadForeignKeys(allTables);

        // Build incoming FK index (table fullName -> list of FKs that reference it)
        Map<String, List<FkWithTable>> incoming = new HashMap<>();
        for (MetaTable t : allTables) {
            List<MetaForeignKey> fks = fksByTableId.getOrDefault(t.getId(), List.of());
            for (MetaForeignKey fk : fks) {
                FkWithTable fkwt = new FkWithTable();
                fkwt.fk = fk;
                fkwt.table = t;
                incoming.computeIfAbsent(fk.getPkTableFullName(), k -> new ArrayList<>()).add(fkwt);
            }
        }

        // BFS from root table following incoming FK direction
        List<AffectedTable> directlyAffected = new ArrayList<>();
        List<AffectedTable> transitivelyAffected = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<Map.Entry<String, Integer>> queue = new LinkedList<>();
        visited.add(rootTable.getFullName());
        queue.add(Map.entry(rootTable.getFullName(), 0));

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> entry = queue.poll();
            String current = entry.getKey();
            int currentDepth = entry.getValue();

            for (FkWithTable fkwt : incoming.getOrDefault(current, List.of())) {
                String childFullName = fkwt.table.getFullName();
                AffectedTable at = new AffectedTable();
                at.setTableName(fkwt.table.getTableName());
                at.setTableFullName(childFullName);
                at.setFkName(fkwt.fk.getFkName());
                at.setFkColumn(fkwt.fk.getFkColumnName());
                at.setPkColumn(fkwt.fk.getPkColumnName());
                at.setUpdateRule(fkwt.fk.getUpdateRule());
                at.setDeleteRule(fkwt.fk.getDeleteRule());
                at.setDepth(currentDepth + 1);

                if (currentDepth == 0) {
                    directlyAffected.add(at);
                } else {
                    transitivelyAffected.add(at);
                }

                if (!visited.contains(childFullName)) {
                    visited.add(childFullName);
                    queue.add(Map.entry(childFullName, currentDepth + 1));
                }
            }
        }

        ImpactAnalysisResponse response = new ImpactAnalysisResponse();
        response.setTableName(rootTable.getTableName());
        response.setTableFullName(rootTable.getFullName());
        response.setDirectlyAffected(directlyAffected);
        response.setTransitivelyAffected(transitivelyAffected);
        response.setTotalAffectedCount(directlyAffected.size() + transitivelyAffected.size());
        return response;
    }

    private Map<Long, List<MetaForeignKey>> loadForeignKeys(List<MetaTable> tables) {
        Map<Long, List<MetaForeignKey>> result = new HashMap<>();
        for (MetaTable t : tables) {
            List<MetaForeignKey> fks = metaForeignKeyRepository.findByTableId(t.getId());
            if (!fks.isEmpty()) {
                result.put(t.getId(), fks);
            }
        }
        return result;
    }

    private static class FkWithTable {
        MetaForeignKey fk;
        MetaTable table;
    }
}
