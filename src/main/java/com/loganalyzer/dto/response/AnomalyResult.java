package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnomalyResult {

    private String type;

    private String message;

    private int severity;
}