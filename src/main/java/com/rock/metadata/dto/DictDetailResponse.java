package com.rock.metadata.dto;

import com.rock.metadata.model.DictDefinition;
import com.rock.metadata.model.DictItem;
import lombok.Data;
import java.util.List;

@Data
public class DictDetailResponse {

    private DictDefinition definition;
    private List<DictItem> items;
}
