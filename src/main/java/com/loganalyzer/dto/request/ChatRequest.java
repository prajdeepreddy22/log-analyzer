package com.loganalyzer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Upload ID is required")
    private String uploadId;

    @NotBlank(message = "Question is required")
    private String question;
}