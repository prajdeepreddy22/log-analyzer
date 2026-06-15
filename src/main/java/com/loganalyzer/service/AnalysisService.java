package com.loganalyzer.service;

import com.loganalyzer.dto.ai.AIJobDto;
import com.loganalyzer.dto.response.AnalysisHistoryResponse;
import com.loganalyzer.dto.response.AnalysisResponse;
import com.loganalyzer.dto.response.AnalysisTriggerResponse;
import com.loganalyzer.dto.response.RateLimitStatus;
import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.AnalysisRepository;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final AnalysisPersistenceService analysisPersistenceService;
    private final LogRepository logRepository;
    private final UploadRepository uploadRepository;
    private final HashKeyService hashKeyService;
    private final AIQueueService aiQueueService;
    private final CacheDecisionService cacheDecisionService;
    private final RateLimitService rateLimitService;
    private final MetricsService metricsService;
    private final ConfidenceScoreService confidenceScoreService;

    // =====================================================
    // MAIN ANALYSIS API
    // =====================================================
    public AnalysisTriggerResponse analyze(
            String uploadId,
            Long userId,
            boolean force
    ) {

        log.info(
                "Analysis request received uploadId={} userId={} force={}",
                uploadId,
                userId,
                force
        );

        Upload upload = uploadRepository
                .findByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Upload not found")
                );

        // =====================================================
        // FETCH IMPORTANT LOGS
        // =====================================================
        List<Log> logs = logRepository
                .findTop100ByUploadUploadIdAndLevelInOrderByLogTimestampDesc(
                        uploadId,
                        List.of(
                                Log.LogLevel.FATAL,
                                Log.LogLevel.ERROR,
                                Log.LogLevel.WARN,
                                Log.LogLevel.UNKNOWN
                        )
                );

        logs = logs.stream()
                .sorted(
                        Comparator.comparing(
                                        (Log log) -> log.getLevel() != null
                                                ? log.getLevel().getSeverity()
                                                : 0
                                )
                                .reversed()
                                .thenComparing(
                                        Log::getLogTimestamp,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                )
                .limit(100)
                .toList();

        if (logs.isEmpty()) {

            throw new ResourceNotFoundException(
                    "No FATAL, ERROR, WARN or UNKNOWN logs found to analyze"
            );
        }

        // =====================================================
        // ENRICH CONTEXT
        // =====================================================
        logs = enrichWithContext(logs);

        // =====================================================
        // HASH
        // =====================================================
        String hash =
                hashKeyService.computeHashFromLogs(logs);

        log.info(
                "Generated hash={} uploadId={}",
                hash,
                uploadId
        );

        // =====================================================
        // FORCE FLOW
        // =====================================================
        if (force) {

            Optional<Analysis> existingOpt =
                    analysisRepository
                            .findByUploadUploadIdAndUserId(
                                    uploadId,
                                    userId
                            );

            if (existingOpt.isPresent()) {

                Analysis existing = existingOpt.get();

                if (existing.getAnalysisStatus()
                        == Analysis.AnalysisStatus.PROCESSING
                        || existing.getAnalysisStatus()
                        == Analysis.AnalysisStatus.PENDING
                        || existing.getAnalysisStatus()
                        == Analysis.AnalysisStatus.RETRYING) {

                    if (!aiQueueService.isActive(existing.getHashKey())) {
                        return recoverStaleAnalysis(
                                existing,
                                uploadId,
                                userId,
                                logs
                        );
                    }

                    return AnalysisTriggerResponse.builder()
                            .status(
                                    existing.getAnalysisStatus().name()
                            )
                            .message(
                                    "Analysis already in progress"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();
                }
            }

            // =====================================================
            // DUPLICATE ACTIVE CHECK
            // =====================================================
            if (aiQueueService.isActive(hash)) {

                return AnalysisTriggerResponse.builder()
                        .status("PROCESSING")
                        .message("Analysis already running")
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();
            }

            // =====================================================
            // RATE LIMIT CHECK
            // =====================================================
            rateLimitService.checkLimit(userId);

            // =====================================================
            // UPSERT ANALYSIS
            // =====================================================
            Analysis analysis =
                    upsertAnalysis(upload, hash);

            analysis.setHashKey(hash);

            analysis.setAnalysisStatus(
                    Analysis.AnalysisStatus.PENDING
            );

            analysis.setRetryCount(0);

            analysis.setErrorMessage(null);

            // clear old AI response
            analysis.setSummary(null);
            analysis.setRootCause(null);
            analysis.setDeveloperMistake(null);
            analysis.setFixSuggestion(null);
            analysis.setCodeFix(null);
            analysis.setSeverityScore(null);
            analysis.setConfidenceScore(null);

            analysisPersistenceService.save(analysis);

            // =====================================================
            // ENQUEUE
            // =====================================================
            enqueueAIJob(
                    uploadId,
                    userId,
                    hash,
                    logs
            );

            return AnalysisTriggerResponse.builder()
                    .status("QUEUED")
                    .message(
                            "Forced analysis queued successfully"
                    )
                    .uploadId(uploadId)
                    .canForce(false)
                    .build();
        }

        // =====================================================
        // EXISTING ANALYSIS CHECK
        // =====================================================
        Optional<Analysis> existingOpt =
                analysisRepository
                        .findByUploadUploadIdAndUserId(
                                uploadId,
                                userId
                        );

        if (existingOpt.isPresent()) {

            Analysis existing = existingOpt.get();

            switch (existing.getAnalysisStatus()) {

                case COMPLETED:

                    return AnalysisTriggerResponse.builder()
                            .status("COMPLETED")
                            .message(
                                    "Analysis already completed"
                            )
                            .uploadId(uploadId)
                            .canForce(true)
                            .build();

                case PROCESSING:

                    if (!aiQueueService.isActive(existing.getHashKey())) {
                        return recoverStaleAnalysis(
                                existing,
                                uploadId,
                                userId,
                                logs
                        );
                    }

                    return AnalysisTriggerResponse.builder()
                            .status("PROCESSING")
                            .message(
                                    "Analysis already processing"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();

                case PENDING:

                    if (!aiQueueService.isActive(existing.getHashKey())) {
                        return recoverStaleAnalysis(
                                existing,
                                uploadId,
                                userId,
                                logs
                        );
                    }

                    return AnalysisTriggerResponse.builder()
                            .status("QUEUED")
                            .message(
                                    "Analysis already queued"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();

                case RETRYING:

                    if (!aiQueueService.isActive(existing.getHashKey())) {
                        return recoverStaleAnalysis(
                                existing,
                                uploadId,
                                userId,
                                logs
                        );
                    }

                    return AnalysisTriggerResponse.builder()
                            .status("RETRYING")
                            .message(
                                    "Retry already in progress"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();

                case FAILED:

                    if (aiQueueService.isActive(
                            existing.getHashKey()
                    )) {

                        return AnalysisTriggerResponse.builder()
                                .status("PROCESSING")
                                .message(
                                        "Analysis already running"
                                )
                                .uploadId(uploadId)
                                .canForce(false)
                                .build();
                    }

                    rateLimitService.checkLimit(userId);

                    existing.setAnalysisStatus(
                            Analysis.AnalysisStatus.PENDING
                    );

                    existing.setRetryCount(
                            existing.getRetryCount() + 1
                    );

                    existing.setErrorMessage(null);

                    analysisPersistenceService.save(existing);

                    enqueueAIJob(
                            uploadId,
                            userId,
                            existing.getHashKey(),
                            logs
                    );

                    return AnalysisTriggerResponse.builder()
                            .status("RETRY")
                            .message(
                                    "Retry analysis queued"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();

                default:
                    break;
            }
        }

        // =====================================================
        // CACHE DECISION
        // =====================================================
        CacheDecisionService.CacheDecision decision =
                cacheDecisionService.decide(hash, userId);

        switch (decision) {

            // =====================================================
            // CACHE HIT
            // =====================================================
            case SKIP: {

                Optional<Analysis> cachedOpt =
                        analysisRepository
                                .findFirstByHashKeyAndUserIdAndAnalysisStatusOrderByUpdatedAtDesc(
                                        hash,
                                        userId,
                                        Analysis.AnalysisStatus.COMPLETED
                                );

                Analysis newAnalysis =
                        upsertAnalysis(upload, hash);

                // =================================================
                // REUSE CACHE
                // =================================================
                if (cachedOpt.isPresent()) {

                    Analysis cached = cachedOpt.get();

                    newAnalysis.setSummary(
                            cached.getSummary()
                    );

                    newAnalysis.setRootCause(
                            cached.getRootCause()
                    );

                    newAnalysis.setDeveloperMistake(
                            cached.getDeveloperMistake()
                    );

                    newAnalysis.setFixSuggestion(
                            cached.getFixSuggestion()
                    );

                    newAnalysis.setCodeFix(
                            cached.getCodeFix()
                    );

                    newAnalysis.setSeverityScore(
                            cached.getSeverityScore()
                    );

                    newAnalysis.setConfidenceScore(
                            cached.getConfidenceScore() != null
                                    ? cached.getConfidenceScore()
                                    : confidenceScoreService.calculate(
                                            logs,
                                            cached.getRootCause()
                                    )
                    );

                    newAnalysis.setAnalysisStatus(
                            Analysis.AnalysisStatus.COMPLETED
                    );

                    newAnalysis.setErrorMessage(null);

                    analysisPersistenceService.save(newAnalysis);

                    metricsService
                            .getCacheHitCounter()
                            .increment();

                    log.info(
                            "Cached analysis reused uploadId={} hash={}",
                            uploadId,
                            hash
                    );

                    return AnalysisTriggerResponse.builder()
                            .status("CACHED")
                            .message(
                                    "Cached analysis reused"
                            )
                            .uploadId(uploadId)
                            .canForce(true)
                            .build();
                }

                // =================================================
                // ACTIVE CHECK
                // =================================================
                if (aiQueueService.isActive(hash)) {

                    return AnalysisTriggerResponse.builder()
                            .status("PROCESSING")
                            .message(
                                    "Analysis already running"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();
                }

                // =================================================
                // RATE LIMIT
                // =================================================
                rateLimitService.checkLimit(userId);

                newAnalysis.setAnalysisStatus(
                        Analysis.AnalysisStatus.PENDING
                );

                newAnalysis.setRetryCount(0);

                newAnalysis.setErrorMessage(null);

                analysisPersistenceService.save(newAnalysis);

                enqueueAIJob(
                        uploadId,
                        userId,
                        hash,
                        logs
                );

                return AnalysisTriggerResponse.builder()
                        .status("QUEUED")
                        .message(
                                "Analysis queued successfully"
                        )
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();
            }

            // =====================================================
            // SIMILAR ANALYSIS RUNNING
            // =====================================================
            case IN_PROGRESS:

                return AnalysisTriggerResponse.builder()
                        .status("PROCESSING")
                        .message(
                                "Similar analysis already running"
                        )
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();

            // =====================================================
            // RETRY FLOW
            // =====================================================
            case RETRY: {

                if (aiQueueService.isActive(hash)) {

                    return AnalysisTriggerResponse.builder()
                            .status("PROCESSING")
                            .message(
                                    "Analysis already running"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();
                }

                rateLimitService.checkLimit(userId);

                Analysis retryAnalysis =
                        upsertAnalysis(upload, hash);

                retryAnalysis.setAnalysisStatus(
                        Analysis.AnalysisStatus.PENDING
                );

                retryAnalysis.setRetryCount(
                        retryAnalysis.getRetryCount() + 1
                );

                retryAnalysis.setErrorMessage(null);

                analysisPersistenceService.save(retryAnalysis);

                enqueueAIJob(
                        uploadId,
                        userId,
                        hash,
                        logs
                );

                return AnalysisTriggerResponse.builder()
                        .status("RETRY")
                        .message(
                                "Retry analysis queued"
                        )
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();
            }

            // =====================================================
            // BRAND NEW ANALYSIS
            // =====================================================
            case NEW: {

                if (aiQueueService.isActive(hash)) {

                    return AnalysisTriggerResponse.builder()
                            .status("PROCESSING")
                            .message(
                                    "Analysis already running"
                            )
                            .uploadId(uploadId)
                            .canForce(false)
                            .build();
                }

                rateLimitService.checkLimit(userId);

                Analysis analysis =
                        upsertAnalysis(upload, hash);

                analysis.setAnalysisStatus(
                        Analysis.AnalysisStatus.PENDING
                );

                analysis.setRetryCount(0);

                analysis.setErrorMessage(null);

                analysisPersistenceService.save(analysis);

                enqueueAIJob(
                        uploadId,
                        userId,
                        hash,
                        logs
                );

                return AnalysisTriggerResponse.builder()
                        .status("QUEUED")
                        .message(
                                "Analysis queued successfully"
                        )
                        .uploadId(uploadId)
                        .canForce(false)
                        .build();
            }

            default:
                throw new IllegalStateException(
                        "Unexpected cache decision"
                );
        }
    }

    // =====================================================
    // GET ANALYSIS
    // =====================================================
    public AnalysisResponse getAnalysis(
            String uploadId,
            Long userId
    ) {

        uploadRepository
                .findByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Upload not found"
                        )
                );

        Analysis analysis = analysisRepository
                .findByUploadUploadIdAndUserId(
                        uploadId,
                        userId
                )
                .orElse(null);

        if (analysis == null) {

            return AnalysisResponse.builder()
                    .uploadId(uploadId)
                    .status("NOT_STARTED")
                    .analysisStatus("NOT_STARTED")
                    .message("Analysis has not been triggered yet")
                    .completed(false)
                    .hasResult(false)
                    .build();
        }

        String status = analysis.getAnalysisStatus().name();
        boolean completed = analysis.getAnalysisStatus()
                == Analysis.AnalysisStatus.COMPLETED;
        boolean hasResult = completed
                && analysis.getSummary() != null
                && !analysis.getSummary().isBlank();

        return AnalysisResponse.builder()
                .uploadId(uploadId)
                .summary(analysis.getSummary())
                .rootCause(analysis.getRootCause())
                .developerMistake(
                        analysis.getDeveloperMistake()
                )
                .fixSuggestion(
                        analysis.getFixSuggestion()
                )
                .codeFix(analysis.getCodeFix())
                .severityScore(
                        SeverityScoreMapper.toApiValue(
                                analysis.getSeverityScore()
                        )
                )
                .confidenceScore(
                        ConfidenceScoreMapper.toApiValue(
                                analysis.getConfidenceScore()
                        )
                )
                .status(status)
                .analysisStatus(status)
                .message(buildAnalysisMessage(
                        status,
                        hasResult,
                        analysis.getErrorMessage()
                ))
                .errorMessage(
                        analysis.getAnalysisStatus()
                                == Analysis.AnalysisStatus.FAILED
                                ? analysis.getErrorMessage()
                                : null
                )
                .completed(completed)
                .hasResult(hasResult)
                .build();
    }

    // =====================================================
    // GET STATUS
    // =====================================================
    public String getStatus(
            String uploadId,
            Long userId
    ) {

        uploadRepository
                .findByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Upload not found"
                        )
                );

        return analysisRepository
                .findStatusByUploadIdAndUserId(
                        uploadId,
                        userId
                )
                .map(Enum::name)
                .orElse("NOT_STARTED");
    }

    // =====================================================
    // GET RATE LIMIT STATUS
    // =====================================================
    public RateLimitStatus getRateLimitStatus(
            Long userId
    ) {

        return rateLimitService.getStatus(userId);
    }

    // =====================================================
    // GET HISTORY
    // =====================================================
    public List<AnalysisHistoryResponse> getHistory(
            Long userId
    ) {

        return analysisRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(a ->
                        AnalysisHistoryResponse.builder()
                                .uploadId(
                                        a.getUpload().getUploadId()
                                )
                                .status(
                                        a.getAnalysisStatus().name()
                                )
                                .severityScore(
                                        a.getSeverityScore() != null
                                                ? SeverityScoreMapper.toApiValue(
                                                        a.getSeverityScore()
                                                )
                                                : 0
                                )
                                .confidenceScore(
                                        ConfidenceScoreMapper.toApiValue(
                                                a.getConfidenceScore()
                                        )
                                )
                                .createdAt(a.getCreatedAt())
                                .build()
                )
                .toList();
    }

    // =====================================================
    // UPSERT ANALYSIS
    // =====================================================
    private Analysis upsertAnalysis(
            Upload upload,
            String hash
    ) {

        return analysisRepository
                .findByUploadUploadIdAndUserId(
                        upload.getUploadId(),
                        upload.getUser().getId()
                )
                .map(existing -> {

                    existing.setHashKey(hash);

                    existing.setErrorMessage(null);

                    return existing;
                })
                .orElseGet(() ->

                        Analysis.builder()
                                .upload(upload)
                                .user(upload.getUser())
                                .hashKey(hash)
                                .promptVersion(2)
                                .retryCount(0)
                                .analysisStatus(
                                        Analysis.AnalysisStatus.PENDING
                                )
                                .build()
                );
    }

    private AnalysisTriggerResponse recoverStaleAnalysis(
            Analysis analysis,
            String uploadId,
            Long userId,
            List<Log> logs
    ) {

        log.warn(
                "Recovering stale analysis job uploadId={} previousStatus={} hash={}",
                uploadId,
                analysis.getAnalysisStatus(),
                analysis.getHashKey()
        );

        analysis.setAnalysisStatus(Analysis.AnalysisStatus.PENDING);
        analysis.setErrorMessage(null);

        analysisPersistenceService.save(analysis);

        enqueueAIJob(
                uploadId,
                userId,
                analysis.getHashKey(),
                logs
        );

        return AnalysisTriggerResponse.builder()
                .status("QUEUED")
                .message("Analysis queue recovered after restart")
                .uploadId(uploadId)
                .canForce(false)
                .build();
    }

    private String buildAnalysisMessage(
            String status,
            boolean hasResult,
            String failureReason
    ) {

        if (hasResult) {
            return "Analysis completed";
        }

        return switch (status) {
            case "PENDING" -> "Analysis is queued. Please wait for AI processing to start.";
            case "PROCESSING" -> "Analysis is in progress. Results will be available shortly.";
            case "RETRYING" -> "Analysis retry is in progress.";
            case "FAILED" -> failureReason == null || failureReason.isBlank()
                    ? "Analysis failed. Please retry analysis."
                    : "Analysis failed: " + failureReason;
            default -> "Analysis result is not available yet.";
        };
    }

    // =====================================================
    // ENQUEUE AI JOB
    // =====================================================
    private void enqueueAIJob(
            String uploadId,
            Long userId,
            String hash,
            List<Log> logs
    ) {

        AIJobDto job = AIJobDto.builder()
                .uploadId(uploadId)
                .userId(userId)
                .hash(hash)
                .logs(logs)
                .build();

        aiQueueService.enqueue(job);

        log.info(
                "AI enqueue uploadId={} queueSize={}",
                uploadId,
                aiQueueService.size()
        );
    }

    // =====================================================
    // ENRICH CONTEXT
    // =====================================================
    private List<Log> enrichWithContext(
            List<Log> errorLogs
    ) {

        if (errorLogs.isEmpty()) {
            return errorLogs;
        }

        String uploadId =
                errorLogs.get(0)
                        .getUpload()
                        .getUploadId();

        List<Log> allLogs =
                logRepository
                        .findTop500ByUploadUploadIdOrderByLogTimestampAsc(
                                uploadId
                        );

        Set<Log> enriched =
                new LinkedHashSet<>();

        for (Log errorLog : errorLogs) {

            int index =
                    allLogs.indexOf(errorLog);

            if (index == -1) {

                enriched.add(errorLog);

                continue;
            }

            int start =
                    Math.max(0, index - 5);

            int end =
                    Math.min(
                            allLogs.size(),
                            index + 6
                    );

            enriched.addAll(
                    allLogs.subList(start, end)
            );
        }

        return enriched.stream()
                .sorted(
                        Comparator.comparing(
                                Log::getLogTimestamp,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .toList();
    }
}
