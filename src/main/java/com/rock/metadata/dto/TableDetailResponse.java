package com.rock.metadata.dto;

import com.rock.metadata.model.*;
import lombok.Data;
import java.util.List;

@Data
public class TableDetailResponse {

    private MetaTable table;
    private List<MetaColumn> columns;
    private List<MetaPrimaryKey> primaryKeys;
    private List<MetaForeignKey> foreignKeys;
    private List<MetaIndex> indexes;
}
