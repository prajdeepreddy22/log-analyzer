package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResponse {

    private String summary;
    private String rootCause;
    private String developerMistake;
    private String fixSuggestion;
    private String codeFix;
    private Integer severityScore;
    private String status;
}