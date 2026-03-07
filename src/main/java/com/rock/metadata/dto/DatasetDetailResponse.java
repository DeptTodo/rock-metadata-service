package com.rock.metadata.dto;

import com.rock.metadata.model.*;
import lombok.Data;

import java.util.List;

@Data
public class DatasetDetailResponse {

    private DatasetDefinition definition;

    private List<DatasetNode> nodes;

    private List<DatasetNodeRelation> relations;

    private List<DatasetNodeFilter> filters;

    private List<DatasetFieldMapping> fieldMappings;
}
