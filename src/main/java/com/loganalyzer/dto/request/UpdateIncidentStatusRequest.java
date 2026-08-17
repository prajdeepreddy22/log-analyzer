package com.loganalyzer.dto.request;

import com.loganalyzer.entity.Incident.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateIncidentStatusRequest {

    @NotNull(message = "is required")
    private IncidentStatus newStatus;

    @Size(max = 500, message = "must be 500 characters or fewer")
    private String note;
}
