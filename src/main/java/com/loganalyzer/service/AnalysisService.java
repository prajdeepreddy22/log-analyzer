package com.loganalyzer.service;

import com.loganalyzer.dto.response.*;
import com.loganalyzer.entity.*;
import com.loganalyzer.entity.Log.LogLevel;
import com.loganalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final LogRepository logRepository;
    private final UploadRepository uploadRepository;
    private final HashKeyService hashKeyService;
    private final AIProcessingService aiProcessingService;
    private final CacheDecisionService cacheDecisionService;

    // =========================
    // Trigger analysis
    // =========================
    public AnalysisTriggerResponse analyze(String uploadId, Long userId, boolean force) {

        log.info("Analysis request received. uploadId={}, userId={}, force={}", uploadId, userId, force);

        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload not found for uploadId=" + uploadId));

        List<Log> logs = logRepository
                .findTop100ByUploadUploadIdAndLevelInOrderByLogTimestampDesc(
                        uploadId,
                        List.of(LogLevel.ERROR, LogLevel.WARN)
                );

        if (logs.isEmpty()) {
            throw new RuntimeException("No logs to analyze for uploadId=" + uploadId);
        }

        String hash = hashKeyService.computeHashFromLogs(logs);

        // =========================
        // FORCE MODE
        // =========================
        if (force) {

            Analysis analysis = analysisRepository
                    .findByHashKeyAndUserId(hash, userId)
                    .orElse(null);

            if (analysis == null) {
                analysis = createNewAnalysis(upload, hash);
            } else {

                if (analysis.getAnalysisStatus() == Analysis.AnalysisStatus.PROCESSING) {
                    return AnalysisTriggerResponse.builder()
                            .status("PROCESSING")
                            .message("Analysis already in progress")
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();
                }

                analysis.setAnalysisStatus(Analysis.AnalysisStatus.PENDING);
                analysis.setRetryCount(0);
                analysis.setErrorMessage(null);
            }

            analysisRepository.save(analysis);

            aiProcessingService.processAnalysis(uploadId, userId, hash, logs);

            return AnalysisTriggerResponse.builder()
                    .status("FORCED")
                    .message("Forced analysis started")
                    .uploadId(uploadId)
                    .canForce(false)
                    .build();
        }

        // =========================
        // CACHE FLOW
        // =========================
        CacheDecisionService.CacheDecision decision =
                cacheDecisionService.decide(hash, userId);

        log.info("Cache decision = {}", decision);

        switch (decision) {

            case SKIP:
                return AnalysisTriggerResponse.builder()
                        .status("CACHED")
                        .message("Analysis already exists")
                        .uploadId(uploadId)
                        .canForce(true)
                        .build();

            case IN_PROGRESS:
                return AnalysisTriggerResponse.builder()
                        .status("PROCESSING")
                        .message("Analysis already in progress")
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();

            case RETRY:
                aiProcessingService.processAnalysis(uploadId, userId, hash, logs);
                return AnalysisTriggerResponse.builder()
                        .status("RETRY")
                        .message("Retrying analysis")
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();

            case NEW:
                Analysis analysis = createNewAnalysis(upload, hash);
                analysisRepository.save(analysis);

                aiProcessingService.processAnalysis(uploadId, userId, hash, logs);

                return AnalysisTriggerResponse.builder()
                        .status("NEW")
                        .message("Analysis started")
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();
        }

        throw new RuntimeException("Unexpected state");
    }

    // =========================
    // Get full analysis
    // =========================
    public AnalysisResponse getAnalysis(String uploadId, Long userId) {

        Analysis analysis = analysisRepository
                .findByUploadUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() -> new RuntimeException("Analysis not found for uploadId=" + uploadId));

        return AnalysisResponse.builder()
                .summary(analysis.getSummary())
                .rootCause(analysis.getRootCause())
                .developerMistake(analysis.getDeveloperMistake())
                .fixSuggestion(analysis.getFixSuggestion())
                .codeFix(analysis.getCodeFix())
                .severityScore(analysis.getSeverityScore())
                .status(analysis.getAnalysisStatus().name())
                .build();
    }

    // =========================
    // Get status
    // =========================
    public String getStatus(String uploadId, Long userId) {

        return analysisRepository
                .findStatusByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() -> new RuntimeException("Analysis not found for uploadId=" + uploadId))
                .name();
    }

    // =========================
    // HISTORY FEATURE
    // =========================
    public List<AnalysisHistoryResponse> getHistory(Long userId) {

        List<Analysis> analyses =
                analysisRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return analyses.stream()
                .map(a -> AnalysisHistoryResponse.builder()
                        .uploadId(a.getUpload().getUploadId())
                        .status(a.getAnalysisStatus().name())
                        .severityScore(a.getSeverityScore() != null ? a.getSeverityScore() : 0)
                        .createdAt(a.getCreatedAt())
                        .build()
                )
                .toList();
    }

    // =========================
    private Analysis createNewAnalysis(Upload upload, String hash) {
        return Analysis.builder()
                .upload(upload)
                .user(upload.getUser())
                .hashKey(hash)
                .promptVersion(2)
                .analysisStatus(Analysis.AnalysisStatus.PENDING)
                .build();
    }
}