package com.rock.metadata.dto;

import com.rock.metadata.model.MetaRoutine;
import com.rock.metadata.model.MetaRoutineColumn;
import lombok.Data;
import java.util.List;

@Data
public class RoutineDetailResponse {

    private MetaRoutine routine;
    private List<MetaRoutineColumn> columns;
}
