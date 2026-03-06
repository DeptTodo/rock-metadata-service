package com.rock.metadata.controller;

import com.rock.metadata.dto.DataSourceRequest;
import com.rock.metadata.model.DataSourceConfig;
import com.rock.metadata.repository.DataSourceConfigRepository;
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
        repository.deleteById(id);
    }
}
