package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentResponse {

    private String incidentId;
    private String uploadId;
    private Long logSourceId;
    private String title;
    private String status;
    private String rootCause;
    private String rootCauseSummary;
    private Integer severityScore;
    private Double confidenceScore;
    private Integer occurrenceCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
}
