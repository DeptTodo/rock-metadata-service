package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.SqlExecuteResponse;
import com.rock.metadata.service.SqlExecuteService;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import static com.rock.metadata.mcp.tool.McpResponseHelper.*;

@Component
@RequiredArgsConstructor
public class SqlTools {

    private final SqlExecuteService sqlExecuteService;

    @McpTool(description = "Execute a SQL query or statement on a datasource. " +
            "Returns columns and rows for queries, or affected row count for statements. " +
            "Default max rows is " + MCP_DEFAULT_SQL_ROWS + " (max " + MCP_MAX_SQL_ROWS + "). " +
            "Cell values exceeding " + CELL_VALUE + " chars are truncated. " +
            "Use with caution: this executes raw SQL against the target database.")
    public SqlExecuteResponse execute_sql(
            @McpToolParam(description = "Datasource ID") Long datasourceId,
            @McpToolParam(description = "SQL query or statement to execute") String sql,
            @McpToolParam(description = "Max rows to return (default " + MCP_DEFAULT_SQL_ROWS + ", max " + MCP_MAX_SQL_ROWS + ")",
                    required = false) Integer maxRows) {
        return ToolExecutor.run("execute SQL", () -> {
            int limit = MCP_DEFAULT_SQL_ROWS;
            if (maxRows != null && maxRows > 0) {
                limit = Math.min(maxRows, MCP_MAX_SQL_ROWS);
            }
            SqlExecuteResponse response = sqlExecuteService.execute(datasourceId, sql, limit);
            // Truncate long cell values
            if (response.getRows() != null) {
                response.setRows(response.getRows().stream()
                        .map(row -> truncateRow(row, CELL_VALUE))
                        .toList());
            }
            return response;
        });
    }
}
