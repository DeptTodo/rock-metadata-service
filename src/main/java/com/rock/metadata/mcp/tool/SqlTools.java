package com.rock.metadata.mcp.tool;

import com.rock.metadata.dto.SqlExecuteResponse;
import com.rock.metadata.service.SqlExecuteService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SqlTools {

    private final SqlExecuteService sqlExecuteService;

    @Tool(description = "Execute a SQL query or statement on a datasource. " +
            "Returns columns and rows for queries, or affected row count for statements. " +
            "Use with caution: this executes raw SQL against the target database.")
    public SqlExecuteResponse execute_sql(
            @ToolParam(description = "Datasource ID") Long datasourceId,
            @ToolParam(description = "SQL query or statement to execute") String sql) {
        return ToolExecutor.run("execute SQL", () ->
                sqlExecuteService.execute(datasourceId, sql));
    }
}
