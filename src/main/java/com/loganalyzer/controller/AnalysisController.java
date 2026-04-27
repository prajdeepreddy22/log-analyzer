package com.loganalyzer.controller;

import com.loganalyzer.dto.response.AnalysisResponse;
import com.loganalyzer.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    // Trigger analysis
    @PostMapping("/{uploadId}")
    public Map<String, Object> analyze(@PathVariable String uploadId,
                                       @RequestHeader("userId") Long userId) {

        analysisService.analyze(uploadId, userId);

        return Map.of(
                "message", "Analysis started",
                "uploadId", uploadId
        );
    }

    // Fetch result
    @GetMapping("/{uploadId}")
    public AnalysisResponse getAnalysis(@PathVariable String uploadId,
                                        @RequestHeader("userId") Long userId) {

        return analysisService.getAnalysis(uploadId, userId);
    }

    // Get only status
    @GetMapping("/{uploadId}/status")
    public Map<String, String> getStatus(@PathVariable String uploadId,
                                         @RequestHeader("userId") Long userId) {

        String status = analysisService.getAnalysis(uploadId, userId).getStatus();

        return Map.of("status", status);
    }
}