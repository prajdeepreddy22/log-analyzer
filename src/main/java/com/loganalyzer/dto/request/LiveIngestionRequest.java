package com.loganalyzer.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class LiveIngestionRequest {

    @NotNull(message = "is required")
    private Long sourceId;

    @NotEmpty(message = "must contain at least one line")
    @Size(max = 500, message = "must contain 500 lines or fewer")
    private List<@Size(max = 16_000, message = "must be 16000 characters or fewer") String> lines;

    private Instant batchTimestamp;
}
