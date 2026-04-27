package com.loganalyzer.service;

import com.loganalyzer.dto.response.AnalysisResponse;
import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MockAIClient {

    public AnalysisResponse analyze(List<Log> logs) {

        // Fake AI response
        return AnalysisResponse.builder()
                .summary("Multiple errors detected in logs")
                .rootCause("NullPointerException in AuthService and payment timeout")
                .developerMistake("Null checks missing and improper error handling")
                .fixSuggestion("Add null checks and retry mechanism for payment")
                .codeFix("if(user != null) { ... } else { throw new Exception(); }")
                .severityScore(4)
                .status("COMPLETED") // optional
                .build();
    }
}