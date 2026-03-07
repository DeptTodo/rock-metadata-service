package com.rock.metadata.controller;

import com.rock.metadata.dto.ConnectionTestRequest;
import com.rock.metadata.dto.ConnectionTestResponse;
import com.rock.metadata.dto.DataSourceRequest;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.DataSourceConfigRepository;
import com.rock.metadata.service.ConnectionTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceConfigRepository repository;
    private final ConnectionTestService connectionTestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataSourceConfig create(@Valid @RequestBody DataSourceRequest req) {
        DataSourceConfig ds = new DataSourceConfig();
        ds.setName(req.getName());
        ds.setDescription(req.getDescription());
        ds.setDbType(req.getDbType());
        ds.setHost(req.getHost());
        ds.setPort(req.getPort());
        ds.setDatabaseName(req.getDatabaseName());
        ds.setUsername(req.getUsername());
        ds.setPassword(req.getPassword());
        ds.setJdbcUrl(req.getJdbcUrl());
        ds.setSchemaPatterns(req.getSchemaPatterns());
        return repository.save(ds);
    }

    @GetMapping
    public List<DataSourceConfig> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public DataSourceConfig get(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));
    }

    @PutMapping("/{id}")
    public DataSourceConfig update(@PathVariable Long id,
                                   @Valid @RequestBody DataSourceRequest req) {
        DataSourceConfig ds = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "DataSource not found"));
        ds.setName(req.getName());
        ds.setDescription(req.getDescription());
        ds.setDbType(req.getDbType());
        ds.setHost(req.getHost());
        ds.setPort(req.getPort());
        ds.setDatabaseName(req.getDatabaseName());
        ds.setUsername(req.getUsername());
        ds.setPassword(req.getPassword());
        ds.setJdbcUrl(req.getJdbcUrl());
        ds.setSchemaPatterns(req.getSchemaPatterns());
        return repository.save(ds);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DataSource not found");
        }
        repository.deleteById(id);
    }

    @PostMapping("/{id}/test-connection")
    public ConnectionTestResponse testConnection(@PathVariable Long id) {
        return connectionTestService.testConnection(id);
    }

    @PostMapping("/test-connection")
    public ConnectionTestResponse testConnectionAdhoc(@Valid @RequestBody ConnectionTestRequest req) {
        return connectionTestService.testConnectionAdhoc(
                req.getDbType(), req.getHost(), req.getPort(),
                req.getDatabaseName(), req.getUsername(), req.getPassword(), req.getJdbcUrl());
    }
}
