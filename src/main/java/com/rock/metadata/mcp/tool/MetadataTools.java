package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.SearchResult;
import com.rock.metadata.dto.TableDetailResponse;
import com.rock.metadata.dto.TableRowCount;
import com.rock.metadata.model.MetaColumn;
import com.rock.metadata.model.MetaForeignKey;
import com.rock.metadata.model.MetaIndex;
import com.rock.metadata.model.MetaSchema;
import com.rock.metadata.model.MetaTable;
import com.rock.metadata.service.MetadataQueryService;
import com.rock.metadata.service.SqlExecuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MetadataTools {

    private final MetadataQueryService metadataQueryService;
    private final SqlExecuteService sqlExecuteService;

    @Tool(description = "List all schemas from the latest successful crawl of a datasource")
    public List<MetaSchema> list_schemas(
            @ToolParam(description = "Datasource ID") Long datasourceId) {
        return metadataQueryService.listSchemas(datasourceId);
    }

    @Tool(description = "List tables from the latest successful crawl, optionally filtered by schema name")
    public List<MetaTable> list_tables(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema) {
        return metadataQueryService.listTables(datasourceId, schema);
    }

    @Tool(description = "Get full table detail including columns, primary keys, foreign keys, " +
            "indexes, triggers, constraints, and privileges")
    public TableDetailResponse get_table_detail(
            @ToolParam(description = "Table ID") Long tableId) {
        return metadataQueryService.getTableDetail(tableId);
    }

    @Tool(description = "List all columns of a table ordered by ordinal position")
    public List<MetaColumn> list_columns(
            @ToolParam(description = "Table ID") Long tableId) {
        return metadataQueryService.listColumns(tableId);
    }

    @Tool(description = "List foreign keys of a table")
    public List<MetaForeignKey> list_foreign_keys(
            @ToolParam(description = "Table ID") Long tableId) {
        return metadataQueryService.listForeignKeys(tableId);
    }

    @Tool(description = "List indexes of a table")
    public List<MetaIndex> list_indexes(
            @ToolParam(description = "Table ID") Long tableId) {
        return metadataQueryService.listIndexes(tableId);
    }

    @Tool(description = "Get actual row counts for tables by connecting to the target datasource. " +
            "Optionally filter by schema name or table name. Returns row count for each matching table.")
    public List<TableRowCount> count_table_rows(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Schema name to filter by (optional)", required = false) String schema,
            @ToolParam(description = "Table name to filter by (optional)", required = false) String tableName) {
        List<MetaTable> tables = metadataQueryService.listTables(datasourceId, schema);
        if (tableName != null && !tableName.isBlank()) {
            tables = tables.stream()
                    .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                    .toList();
        }
        return sqlExecuteService.countTableRows(datasourceId, tables);
    }

    @Tool(description = "Search tables and columns by keyword across a datasource's latest crawl")
    public SearchResult search_metadata(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Search keyword") String keyword) {
        return metadataQueryService.search(datasourceId, keyword);
    }
}
