package com.rock.metadata.dto;

import lombok.Data;

@Data
public class CrawlRequest {

    /** minimum, standard, detailed, maximum */
    private String infoLevel = "maximum";
}
