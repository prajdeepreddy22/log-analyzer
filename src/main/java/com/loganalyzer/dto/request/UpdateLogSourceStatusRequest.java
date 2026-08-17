package com.loganalyzer.dto.request;

import com.loganalyzer.entity.LogIngestionSource.SourceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLogSourceStatusRequest {

    @NotNull(message = "is required")
    private SourceStatus status;
}
