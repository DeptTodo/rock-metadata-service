package com.rock.metadata.dto;

import lombok.Data;

@Data
public class UpdateTableAttrsRequest {

    private String displayName;
    private String businessDescription;
    private String businessDomain;
    private String owner;
    /** CORE, IMPORTANT, NORMAL, TRIVIAL */
    private String importanceLevel;
    private Integer dataQualityScore;
}
