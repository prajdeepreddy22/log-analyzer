package com.loganalyzer.controller;

import com.loganalyzer.dto.response.*;
import com.loganalyzer.service.AnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new RuntimeException("Unauthorized: userId missing in request");
        }

        return userId;
    }

    @PostMapping("/{uploadId}")
    public AnalysisTriggerResponse analyze(@PathVariable String uploadId,
                                           @RequestParam(defaultValue = "false") boolean force,
                                           HttpServletRequest request) {

        return analysisService.analyze(uploadId, getUserId(request), force);
    }

    @GetMapping("/{uploadId}")
    public AnalysisResponse getAnalysis(@PathVariable String uploadId,
                                        HttpServletRequest request) {

        return analysisService.getAnalysis(uploadId, getUserId(request));
    }

    @GetMapping("/{uploadId}/status")
    public String getStatus(@PathVariable String uploadId,
                            HttpServletRequest request) {

        return analysisService.getStatus(uploadId, getUserId(request));
    }

    @GetMapping("/history")
    public List<AnalysisHistoryResponse> getHistory(HttpServletRequest request) {
        return analysisService.getHistory(getUserId(request));
    }
}