package com.loganalyzer.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String uploadId;

    private String question;

    private String message;

    private String sessionId;

    public String getQuestion() {

        if (question != null && !question.isBlank()) {
            return question;
        }

        return message;
    }

    @AssertTrue(message = "Upload ID is required")
    public boolean isUploadIdPresent() {
        return uploadId != null && !uploadId.isBlank();
    }

    @AssertTrue(message = "Message is required")
    public boolean isMessagePresent() {
        return getQuestion() != null && !getQuestion().isBlank();
    }
}
