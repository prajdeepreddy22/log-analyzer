package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RuleMatchResult {

    private String category;

    private String description;

    private int severity;

    private boolean matched;
}