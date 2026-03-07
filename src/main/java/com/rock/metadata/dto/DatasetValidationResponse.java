package com.rock.metadata.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DatasetValidationResponse {

    private boolean valid;

    private List<String> errors = new ArrayList<>();

    private List<String> warnings = new ArrayList<>();

    private List<String> executionOrder = new ArrayList<>();
}
