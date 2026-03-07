package com.rock.metadata.dto;

import com.rock.metadata.model.DatasetInstance;
import com.rock.metadata.model.DatasetInstanceSnapshot;
import lombok.Data;

import java.util.List;

@Data
public class DatasetInstanceDetailResponse {

    private DatasetInstance instance;

    private List<DatasetInstanceSnapshot> snapshots;
}
