package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RootCauseResult {
    private String rootCause;
    private int confidence;
}