package com.rock.metadata.service;

import com.rock.metadata.model.*;
import com.rock.metadata.repository.*;
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
public class MetadataExportService {

    private final MetadataQueryService metadataQueryService;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final MetaPrimaryKeyRepository metaPrimaryKeyRepository;
    private final MetaForeignKeyRepository metaForeignKeyRepository;
    private final MetaIndexRepository metaIndexRepository;

    public String exportMetadata(Long datasourceId, String format, String schemaName) {
        List<MetaTable> tables = metadataQueryService.listTables(datasourceId, schemaName);

        return switch (format.toUpperCase()) {
            case "DDL" -> exportAsDdl(tables);
            case "JSON" -> exportAsJson(tables);
            case "MARKDOWN" -> exportAsMarkdown(tables);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported format: " + format + ". Supported: DDL, JSON, MARKDOWN");
        };
    }

    private String exportAsDdl(List<MetaTable> tables) {
        StringBuilder sb = new StringBuilder();
        for (MetaTable table : tables) {
            List<MetaColumn> columns = metaColumnRepository.findByTableIdOrderByOrdinalPosition(table.getId());
            List<MetaPrimaryKey> pks = metaPrimaryKeyRepository.findByTableId(table.getId());
            List<MetaForeignKey> fks = metaForeignKeyRepository.findByTableId(table.getId());
            List<MetaIndex> indexes = metaIndexRepository.findByTableId(table.getId());

            sb.append("CREATE TABLE ").append(table.getFullName()).append(" (\n");

            for (int i = 0; i < columns.size(); i++) {
                MetaColumn col = columns.get(i);
                sb.append("    ").append(col.getColumnName()).append(" ");
                sb.append(col.getDbSpecificTypeName() != null ? col.getDbSpecificTypeName() : col.getDataType());
                if (!col.isNullable()) sb.append(" NOT NULL");
                if (col.getDefaultValue() != null && !col.getDefaultValue().isBlank()) {
                    sb.append(" DEFAULT ").append(col.getDefaultValue());
                }
                if (i < columns.size() - 1 || !pks.isEmpty() || !fks.isEmpty()) sb.append(",");
                sb.append("\n");
            }

            if (!pks.isEmpty()) {
                String pkCols = pks.stream()
                        .sorted(Comparator.comparing(MetaPrimaryKey::getKeySequence))
                        .map(MetaPrimaryKey::getColumnName)
                        .collect(Collectors.joining(", "));
                sb.append("    CONSTRAINT ").append(pks.get(0).getConstraintName())
                  .append(" PRIMARY KEY (").append(pkCols).append(")");
                if (!fks.isEmpty()) sb.append(",");
                sb.append("\n");
            }

            Map<String, List<MetaForeignKey>> fkGroups = fks.stream()
                    .collect(Collectors.groupingBy(MetaForeignKey::getFkName, LinkedHashMap::new, Collectors.toList()));
            int fkIdx = 0;
            for (Map.Entry<String, List<MetaForeignKey>> entry : fkGroups.entrySet()) {
                List<MetaForeignKey> fkCols = entry.getValue();
                String fkColNames = fkCols.stream().map(MetaForeignKey::getFkColumnName)
                        .collect(Collectors.joining(", "));
                String pkColNames = fkCols.stream().map(MetaForeignKey::getPkColumnName)
                        .collect(Collectors.joining(", "));
                sb.append("    CONSTRAINT ").append(entry.getKey())
                  .append(" FOREIGN KEY (").append(fkColNames).append(")")
                  .append(" REFERENCES ").append(fkCols.get(0).getPkTableFullName())
                  .append(" (").append(pkColNames).append(")");
                if (fkIdx < fkGroups.size() - 1) sb.append(",");
                sb.append("\n");
                fkIdx++;
            }

            sb.append(");\n\n");

            Map<String, List<MetaIndex>> idxGroups = indexes.stream()
                    .collect(Collectors.groupingBy(MetaIndex::getIndexName, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<MetaIndex>> entry : idxGroups.entrySet()) {
                List<MetaIndex> idxCols = entry.getValue();
                boolean unique = idxCols.get(0).isUnique();
                String idxColNames = idxCols.stream()
                        .sorted(Comparator.comparing(MetaIndex::getOrdinalPosition))
                        .map(MetaIndex::getColumnName)
                        .collect(Collectors.joining(", "));
                sb.append("CREATE ");
                if (unique) sb.append("UNIQUE ");
                sb.append("INDEX ").append(entry.getKey())
                  .append(" ON ").append(table.getFullName())
                  .append(" (").append(idxColNames).append(");\n");
            }
            if (!indexes.isEmpty()) sb.append("\n");
        }
        return sb.toString();
    }

    private String exportAsJson(List<MetaTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int t = 0; t < tables.size(); t++) {
            MetaTable table = tables.get(t);
            List<MetaColumn> columns = metaColumnRepository.findByTableIdOrderByOrdinalPosition(table.getId());
            List<MetaPrimaryKey> pks = metaPrimaryKeyRepository.findByTableId(table.getId());
            List<MetaForeignKey> fks = metaForeignKeyRepository.findByTableId(table.getId());
            List<MetaIndex> indexes = metaIndexRepository.findByTableId(table.getId());

            sb.append("  {\n");
            sb.append("    \"tableName\": \"").append(escape(table.getTableName())).append("\",\n");
            sb.append("    \"fullName\": \"").append(escape(table.getFullName())).append("\",\n");
            sb.append("    \"schemaName\": \"").append(escape(table.getSchemaName())).append("\",\n");
            sb.append("    \"tableType\": \"").append(escape(table.getTableType())).append("\",\n");
            sb.append("    \"remarks\": ").append(jsonString(table.getRemarks())).append(",\n");

            // Columns
            sb.append("    \"columns\": [\n");
            for (int i = 0; i < columns.size(); i++) {
                MetaColumn col = columns.get(i);
                sb.append("      {\"name\": \"").append(escape(col.getColumnName())).append("\", ");
                sb.append("\"dataType\": \"").append(escape(col.getDataType())).append("\", ");
                sb.append("\"nullable\": ").append(col.isNullable()).append(", ");
                sb.append("\"size\": ").append(col.getColumnSize()).append(", ");
                sb.append("\"primaryKey\": ").append(col.isPartOfPrimaryKey()).append(", ");
                sb.append("\"foreignKey\": ").append(col.isPartOfForeignKey());
                sb.append("}");
                if (i < columns.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ],\n");

            // Primary Keys
            sb.append("    \"primaryKeys\": [");
            sb.append(pks.stream().map(pk -> "\"" + escape(pk.getColumnName()) + "\"")
                    .collect(Collectors.joining(", ")));
            sb.append("],\n");

            // Foreign Keys
            sb.append("    \"foreignKeys\": [\n");
            for (int i = 0; i < fks.size(); i++) {
                MetaForeignKey fk = fks.get(i);
                sb.append("      {\"column\": \"").append(escape(fk.getFkColumnName())).append("\", ");
                sb.append("\"referencesTable\": \"").append(escape(fk.getPkTableFullName())).append("\", ");
                sb.append("\"referencesColumn\": \"").append(escape(fk.getPkColumnName())).append("\"}");
                if (i < fks.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ],\n");

            // Indexes
            sb.append("    \"indexes\": [\n");
            for (int i = 0; i < indexes.size(); i++) {
                MetaIndex idx = indexes.get(i);
                sb.append("      {\"name\": \"").append(escape(idx.getIndexName())).append("\", ");
                sb.append("\"column\": \"").append(escape(idx.getColumnName())).append("\", ");
                sb.append("\"unique\": ").append(idx.isUnique()).append("}");
                if (i < indexes.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ]\n");

            sb.append("  }");
            if (t < tables.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private String exportAsMarkdown(List<MetaTable> tables) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Database Schema Documentation\n\n");

        for (MetaTable table : tables) {
            List<MetaColumn> columns = metaColumnRepository.findByTableIdOrderByOrdinalPosition(table.getId());
            List<MetaPrimaryKey> pks = metaPrimaryKeyRepository.findByTableId(table.getId());
            List<MetaForeignKey> fks = metaForeignKeyRepository.findByTableId(table.getId());

            sb.append("## ").append(table.getFullName()).append("\n\n");
            if (table.getRemarks() != null && !table.getRemarks().isBlank()) {
                sb.append(table.getRemarks()).append("\n\n");
            }
            sb.append("**Type:** ").append(table.getTableType()).append("\n\n");

            // Columns table
            sb.append("| Column | Type | Nullable | PK | FK | Default | Remarks |\n");
            sb.append("|--------|------|----------|----|----|---------|--------|\n");
            for (MetaColumn col : columns) {
                sb.append("| ").append(col.getColumnName());
                sb.append(" | ").append(col.getDataType() != null ? col.getDataType() : "");
                sb.append(" | ").append(col.isNullable() ? "YES" : "NO");
                sb.append(" | ").append(col.isPartOfPrimaryKey() ? "PK" : "");
                sb.append(" | ").append(col.isPartOfForeignKey() ? "FK" : "");
                sb.append(" | ").append(col.getDefaultValue() != null ? col.getDefaultValue() : "");
                sb.append(" | ").append(col.getRemarks() != null ? col.getRemarks() : "");
                sb.append(" |\n");
            }

            if (!fks.isEmpty()) {
                sb.append("\n**Foreign Keys:**\n\n");
                for (MetaForeignKey fk : fks) {
                    sb.append("- `").append(fk.getFkColumnName()).append("` → `")
                      .append(fk.getPkTableFullName()).append(".").append(fk.getPkColumnName()).append("`\n");
                }
            }
            sb.append("\n---\n\n");
        }
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsonString(String s) {
        if (s == null) return "null";
        return "\"" + escape(s) + "\"";
    }
}
