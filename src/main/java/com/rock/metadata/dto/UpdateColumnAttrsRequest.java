package com.rock.metadata.dto;

import lombok.Data;

@Data
public class UpdateColumnAttrsRequest {

    private String displayName;
    private String businessDescription;
    private String businessDataType;
    private String sampleValues;
    private String valueRange;
    /** PUBLIC, INTERNAL, SENSITIVE, HIGHLY_SENSITIVE */
    private String sensitivityLevel;
    private String sensitivityType;
    private String maskingStrategy;
    private String complianceFlags;
}
