package com.rock.metadata.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DatasetExecuteRequest {

    private String rootKeyValue;

    private Map<String, String> params;
}
