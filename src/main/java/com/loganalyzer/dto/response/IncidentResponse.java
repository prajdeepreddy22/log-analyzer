package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentResponse {

    private String incidentId;
    private String uploadId;
    private String rootCause;
    private Integer severityScore;
    private Double confidenceScore;
    private Integer occurrenceCount;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
}
