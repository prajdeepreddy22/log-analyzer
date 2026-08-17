package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentStatusHistoryResponse {

    private Long id;
    private String incidentId;
    private String fromStatus;
    private String toStatus;
    private Long changedBy;
    private LocalDateTime changedAt;
    private String note;
}
