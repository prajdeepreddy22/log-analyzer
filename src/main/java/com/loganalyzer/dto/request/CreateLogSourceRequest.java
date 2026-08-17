package com.loganalyzer.dto.request;

import com.loganalyzer.entity.LogIngestionSource.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLogSourceRequest {

    @NotBlank(message = "is required")
    @Size(max = 255, message = "must be 255 characters or fewer")
    private String sourceName;

    @NotNull(message = "is required")
    private SourceType sourceType;
}
