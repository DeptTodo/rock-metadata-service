package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.ColumnProfile;
import com.rock.metadata.dto.TableProfileResponse;
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
}
