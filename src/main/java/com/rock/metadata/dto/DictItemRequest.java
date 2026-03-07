package com.rock.metadata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DictItemRequest {

    @NotNull
    private Long dictId;

    private Long parentId;

    @NotBlank
    private String itemCode;

    @NotBlank
    private String itemValue;

    private String itemDescription;
    private Integer sortOrder;
    private Integer treeLevel;
    private String extAttrs;
    private Boolean active;
}
