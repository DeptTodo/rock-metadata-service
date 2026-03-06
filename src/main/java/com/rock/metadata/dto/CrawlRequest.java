package com.rock.metadata.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CrawlRequest {

    @Pattern(regexp = "minimum|standard|detailed|maximum",
            message = "infoLevel must be one of: minimum, standard, detailed, maximum")
    private String infoLevel = "maximum";
}
