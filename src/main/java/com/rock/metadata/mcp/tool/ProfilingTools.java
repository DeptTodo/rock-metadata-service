package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.*;
import com.rock.metadata.service.DataProfilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProfilingTools {

    private final DataProfilingService dataProfilingService;

    @Tool(description = "Profile a table by running analysis queries against the live database. " +
            "Returns row count and per-column statistics: distinct count, null count, null percentage, " +
            "min/max values, and sample values. Optionally specify columns to profile.")
    public TableProfileResponse profile_table(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "Specific column names to profile (optional, profiles all if not specified)",
                    required = false) List<String> columns) {
        return dataProfilingService.profileTable(datasourceId, tableId, columns);
    }

    @Tool(description = "Profile a single column of a table against the live database")
    public ColumnProfile profile_column(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "Column name") String columnName) {
        return dataProfilingService.profileSingleColumn(datasourceId, tableId, columnName);
    }

    @Tool(description = "Sample rows from a table in the live database. " +
            "Returns full rows with all columns. Default limit is 10, max 100.")
    public DataSampleResponse sample_table_rows(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "Number of rows to sample (default 10, max 100)",
                    required = false) Integer limit) {
        return dataProfilingService.sampleTableRows(datasourceId, tableId, limit);
    }

    @Tool(description = "Get distinct values of a column with their frequency counts, " +
            "ordered by count descending. Default limit is 50, max 500.")
    public DistinctValueResponse get_distinct_column_values(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "Table ID") Long tableId,
            @ToolParam(description = "Column name") String columnName,
            @ToolParam(description = "Max number of distinct values to return (default 50, max 500)",
                    required = false) Integer limit) {
        return dataProfilingService.getDistinctColumnValues(datasourceId, tableId, columnName, limit);
    }
}
