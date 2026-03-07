package com.rock.metadata.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SqlExecuteResponse {

    /** true if the SQL returned a result set (e.g. SELECT) */
    private boolean query;

    /** Column names, present when query=true */
    private List<String> columns;

    /** Row data as list of column-name -> value maps, present when query=true */
    private List<Map<String, Object>> rows;

    /** Number of affected rows, present when query=false (DML/DDL) */
    private Integer affectedRows;

    /** True if result rows were capped by maxRows limit */
    private Boolean truncated;

    /** Total row count before truncation, if available */
    private Integer returnedRows;
}
