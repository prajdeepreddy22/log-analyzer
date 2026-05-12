package com.loganalyzer.controller;

import com.loganalyzer.dto.batch.BatchAnalysisJob;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.BatchAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/batch")
public class BatchAnalysisController {

    private final BatchAnalysisService batchAnalysisService;

    // =====================================================
    // JWT USER EXTRACTION — matches your existing pattern
    // =====================================================
    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userId;
    }

    // =====================================================
    // START BATCH ANALYSIS
    // =====================================================
    @PostMapping("/analyze")
    public BatchAnalysisJob analyzeBatch(
            @RequestBody List<String> uploadIds,
            HttpServletRequest request
    ) {
        Long userId = getUserId(request);
        log.info("Batch analysis request userId={} uploads={}", userId, uploadIds.size());

        return batchAnalysisService.startBatchAnalysis(uploadIds, userId);
    }

    // =====================================================
    // GET BATCH STATUS
    // =====================================================
    @GetMapping("/{batchId}")
    public BatchAnalysisJob getStatus(
            @PathVariable String batchId,
            HttpServletRequest request
    ) {
        Long userId = getUserId(request);
        log.info("Batch status request batchId={} userId={}", batchId, userId);

        return batchAnalysisService.getBatchStatus(batchId);
    }

    // =====================================================
    // GET ALL BATCHES FOR THIS USER
    // =====================================================
    @GetMapping
    public List<BatchAnalysisJob> getAll(HttpServletRequest request) {
        Long userId = getUserId(request);
        log.info("Fetch all batches userId={}", userId);

        return batchAnalysisService.getAllJobsByUser(userId);
    }
}