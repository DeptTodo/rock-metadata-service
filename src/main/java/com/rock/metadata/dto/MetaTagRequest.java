package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MetaTagRequest {

    @NotBlank
    private String targetType;

    @NotNull
    private Long targetId;

    @NotBlank
    private String tagKey;

    private String tagValue;

    private String source;
}
