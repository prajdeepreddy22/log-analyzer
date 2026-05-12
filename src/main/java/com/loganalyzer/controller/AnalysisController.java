package com.loganalyzer.controller;

import com.loganalyzer.dto.response.AnalysisHistoryResponse;
import com.loganalyzer.dto.response.AnalysisResponse;
import com.loganalyzer.dto.response.AnalysisTriggerResponse;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.AnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalysisController {

    private final AnalysisService analysisService;

    // =====================================================
    // JWT USER EXTRACTION
    // =====================================================
    private Long getUserId(HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userId;
    }

    // =====================================================
    // TRIGGER ANALYSIS
    // =====================================================
    @PostMapping("/{uploadId}")
    public AnalysisTriggerResponse analyze(
            @PathVariable String uploadId,
            @RequestParam(defaultValue = "false") boolean force,
            HttpServletRequest request) {

        Long userId = getUserId(request);

        log.info(
                "Trigger analysis request uploadId={} userId={} force={}",
                uploadId,
                userId,
                force
        );

        return analysisService.analyze(
                uploadId,
                userId,
                force
        );
    }

    // =====================================================
    // GET ANALYSIS RESULT
    // =====================================================
    @GetMapping("/{uploadId}")
    public AnalysisResponse getAnalysis(
            @PathVariable String uploadId,
            HttpServletRequest request) {

        Long userId = getUserId(request);

        log.info(
                "Fetching analysis uploadId={} userId={}",
                uploadId,
                userId
        );

        return analysisService.getAnalysis(
                uploadId,
                userId
        );
    }

    // =====================================================
    // GET STATUS ONLY
    // =====================================================
    @GetMapping("/{uploadId}/status")
    public Map<String, String> getStatus(
            @PathVariable String uploadId,
            HttpServletRequest request) {

        Long userId = getUserId(request);

        log.info(
                "Fetching analysis status uploadId={} userId={}",
                uploadId,
                userId
        );

        return Map.of(
                "status",
                analysisService.getStatus(uploadId, userId)
        );
    }

    // =====================================================
    // GET HISTORY
    // =====================================================
    @GetMapping("/history")
    public List<AnalysisHistoryResponse> getHistory(
            HttpServletRequest request) {

        Long userId = getUserId(request);

        log.info(
                "Fetching analysis history userId={}",
                userId
        );

        return analysisService.getHistory(userId);
    }
}