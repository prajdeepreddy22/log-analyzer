package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResponse {

    private String uploadId;
    private String summary;
    private String rootCause;
    private String developerMistake;
    private String fixSuggestion;
    private String codeFix;
    private Integer severityScore;
    private String status;
    private String analysisStatus;
    private String message;
    private boolean completed;
    private boolean hasResult;
}
