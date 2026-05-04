package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnalysisHistoryResponse {

    private String uploadId;
    private String status;
    private Integer severityScore;
    private LocalDateTime createdAt;
}