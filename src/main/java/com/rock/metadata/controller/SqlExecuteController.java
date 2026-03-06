package com.rock.metadata.controller;

import com.rock.metadata.dto.SqlExecuteRequest;
import com.rock.metadata.dto.SqlExecuteResponse;
import com.rock.metadata.service.SqlExecuteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sql")
@RequiredArgsConstructor
public class SqlExecuteController {

    private final SqlExecuteService sqlExecuteService;

    @PostMapping("/execute")
    public SqlExecuteResponse execute(@Valid @RequestBody SqlExecuteRequest request) {
        return sqlExecuteService.execute(request.getDatasourceId(), request.getSql());
    }
}
