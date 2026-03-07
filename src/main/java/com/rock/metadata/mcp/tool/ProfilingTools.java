package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.*;
import com.rock.metadata.service.DataProfilingService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.rock.metadata.mcp.tool.McpResponseHelper.*;

@Component
@RequiredArgsConstructor
public class ProfilingTools {

    private final DataProfilingService dataProfilingService;

    @McpTool(description = "Profile a table by running analysis queries against the live database. " +
            "Returns row count and per-column statistics: distinct count, null count, null percentage, " +
            "min/max values, and sample values. Optionally specify columns to profile (recommended for wide tables).")
    public TableProfileResponse profile_table(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Table ID") Long tableId,
            @McpToolParam(description = "Specific column names to profile (optional, profiles all if not specified)",
                    required = false) List<String> columns) {
        return ToolExecutor.run("profile table", () ->
                dataProfilingService.profileTable(datasourceId, tableId, columns));
    }

    @McpTool(description = "Profile a single column of a table against the live database")
    public ColumnProfile profile_column(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Table ID") Long tableId,
            @McpToolParam(description = "Column name") String columnName) {
        return ToolExecutor.run("profile column", () ->
                dataProfilingService.profileSingleColumn(datasourceId, tableId, columnName));
    }

    @McpTool(description = "Sample rows from a table in the live database. " +
            "Returns rows with all columns. Cell values exceeding " + CELL_VALUE + " chars are truncated. " +
            "Default limit is " + MCP_DEFAULT_SAMPLE_ROWS + ", max 100.")
    public DataSampleResponse sample_table_rows(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Table ID") Long tableId,
            @McpToolParam(description = "Number of rows to sample (default " + MCP_DEFAULT_SAMPLE_ROWS + ", max 100)",
                    required = false) Integer limit) {
        return ToolExecutor.run("sample table rows", () -> {
            int effectiveLimit = (limit != null && limit > 0) ? limit : MCP_DEFAULT_SAMPLE_ROWS;
            DataSampleResponse response = dataProfilingService.sampleTableRows(datasourceId, tableId, effectiveLimit);
            // Truncate long cell values
            if (response.getRows() != null) {
                response.setRows(response.getRows().stream()
                        .map(row -> truncateRow(row, CELL_VALUE))
                        .toList());
            }
            return response;
        });
    }

    @McpTool(description = "Get distinct values of a column with their frequency counts, " +
            "ordered by count descending. Default limit is 20, max 500.")
    public DistinctValueResponse get_distinct_column_values(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "Table ID") Long tableId,
            @McpToolParam(description = "Column name") String columnName,
            @McpToolParam(description = "Max number of distinct values to return (default 20, max 500)",
                    required = false) Integer limit) {
        return ToolExecutor.run("get distinct column values", () -> {
            int effectiveLimit = (limit != null && limit > 0) ? limit : 20;
            return dataProfilingService.getDistinctColumnValues(datasourceId, tableId, columnName, effectiveLimit);
        });
    }
}
