package com.rock.metadata.dto;

import lombok.Data;

@Data
public class UpdateSchemaAttrsRequest {

    private String displayName;
    private String businessDescription;
    private String owner;
}
