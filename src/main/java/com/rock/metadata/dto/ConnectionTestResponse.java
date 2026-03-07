package com.rock.metadata.dto;

import lombok.Data;

@Data
public class ConnectionTestResponse {

    private boolean success;
    private long responseTimeMs;
    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String errorMessage;
}
