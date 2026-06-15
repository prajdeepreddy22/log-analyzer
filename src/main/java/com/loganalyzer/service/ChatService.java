package com.loganalyzer.service;

import com.loganalyzer.client.OpenAIClient;
import com.loganalyzer.dto.request.ChatRequest;
import com.loganalyzer.dto.response.AnomalyResult;
import com.loganalyzer.dto.response.ChatResponse;
import com.loganalyzer.dto.response.RootCauseResult;
import com.loganalyzer.dto.response.UsedLogDto;
import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.AnalysisRepository;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final UploadRepository uploadRepository;
    private final LogRepository logRepository;
    private final AnalysisRepository analysisRepository;

    private final PromptBuilderService promptBuilderService;
    private final ChatCacheService chatCacheService;
    private final OpenAIClient openAIClient;

    // STEP 10.3
    private final LogRelevanceService logRelevanceService;

    // STEP 10.4
    private final IncidentLogService incidentLogService;

    // STEP 10.5
    private final RootCauseDetectorService rootCauseDetectorService;

    // STEP 10.6
    private final AIConfidenceService aiConfidenceService;

    // STEP 10.6
    private final ResponseQualityService responseQualityService;

    // STEP 10.7
    private final ChatInsightService chatInsightService;

    // STEP 10.8
    private final RulesEngineService rulesEngineService;

    // STEP 10.9
    private final AnomalyDetectionService anomalyDetectionService;

    // STEP 10.10
    private final PromptCompressionService promptCompressionService;

    // STEP 10.10
    private final TokenOptimizationService tokenOptimizationService;

    private final ChatResponseFormatterService chatResponseFormatterService;

    private final RateLimitService rateLimitService;

    public ChatResponse askQuestion(ChatRequest request, Long userId) {

        log.info(
                "Chat request received for uploadId={} by userId={}",
                request.getUploadId(),
                userId
        );

        // =====================================================
        // SECURITY CHECK
        // =====================================================
        validateUploadAccess(request.getUploadId(), userId);

        // =====================================================
        // CACHE KEY
        // =====================================================
        String cacheKey = buildCacheKey(
                request.getUploadId(),
                userId,
                request.getQuestion()
        );

        // =====================================================
        // CACHE CHECK
        // =====================================================
        ChatCacheService.CachedChatResponse cached =
                chatCacheService.get(cacheKey);

        if (cached != null) {

            log.info(
                    "Chat cache hit for uploadId={} userId={}",
                    request.getUploadId(),
                    userId
            );

            List<UsedLogDto> cachedLogs = logRepository
                    .findTop10ByUploadUploadIdOrderByLogTimestampDesc(
                            request.getUploadId()
                    )
                    .stream()
                    .map(log -> UsedLogDto.builder()
                            .id(log.getId())
                            .timestamp(log.getLogTimestamp())
                            .level(log.getLevel().name())
                            .message(log.getMessage())
                            .build())
                    .toList();

            return ChatResponse.builder()
                    .answer(cached.getAnswer())
                    .usedLogs(cachedLogs)
                    .source("CACHED")
                    .confidence(90)
                    .quality("HIGH")
                    .insights(
                            List.of(
                                    "Response served from AI cache",
                                    "Previous analysis reused",
                                    "Token optimized response"
                            )
                    )
                    .build();
        }

        log.info(
                "Chat cache miss for uploadId={} userId={}",
                request.getUploadId(),
                userId
        );

        // =====================================================
        // STEP 1: FETCH RAW LOGS
        // =====================================================
        Pageable pageable = PageRequest.of(0, 300);

        List<Log> rawLogs = logRepository
                .findByUploadUploadIdOrderByLogTimestampDesc(
                        request.getUploadId(),
                        pageable
                )
                .getContent();

        if (rawLogs.isEmpty()) {
            throw new ResourceNotFoundException("No logs found for upload");
        }

        rateLimitService.checkLimit(userId);

        log.info("Fetched {} raw logs", rawLogs.size());

        // =====================================================
        // STEP 2: FILTER LOGS
        // =====================================================
        List<Log> filteredLogs = logRelevanceService
                .filterRelevantLogs(
                        request.getQuestion(),
                        rawLogs
                );

        if (filteredLogs.isEmpty()) {

            log.warn(
                    "No relevant logs found. Falling back to top logs"
            );

            filteredLogs = rawLogs.stream()
                    .limit(50)
                    .toList();
        }

        log.info(
                "Filtered {} relevant logs from {}",
                filteredLogs.size(),
                rawLogs.size()
        );

        // =====================================================
        // STEP 3: INCIDENT RECONSTRUCTION
        // =====================================================
        List<Log> logs = incidentLogService
                .buildIncidentContext(filteredLogs);

        if (logs.isEmpty()) {
            logs = filteredLogs;
        }

        logs = logs.stream()
                .sorted(timestampComparator())
                .toList();

        log.info(
                "Final incident logs prepared: {}",
                logs.size()
        );

        // =====================================================
        // STEP 10.10 - TOKEN OPTIMIZATION
        // =====================================================
        logs = tokenOptimizationService.optimizeLogs(logs);

        log.info(
                "Logs optimized for token usage. Remaining logs={}",
                logs.size()
        );

        // =====================================================
        // STEP 4: ROOT CAUSE DETECTION
        // =====================================================
        RootCauseResult rootCauseResult =
                rootCauseDetectorService.detectRootCause(logs);

        log.info(
                "Root cause detected={} confidence={}",
                rootCauseResult.getRootCause(),
                rootCauseResult.getConfidence()
        );

        // =====================================================
        // STEP 10.8 - RULE ENGINE
        // =====================================================
        List<String> ruleFindings =
                rulesEngineService.evaluate(logs);

        log.info(
                "Rules engine findings count={}",
                ruleFindings.size()
        );

        // =====================================================
        // STEP 10.9 - ANOMALY DETECTION
        // =====================================================
        List<AnomalyResult> anomalyResults =
                anomalyDetectionService.detectAnomalies(logs);

        log.info(
                "Anomalies detected count={}",
                anomalyResults.size()
        );

        // =====================================================
        // CONVERT ANOMALIES TO INSIGHTS
        // =====================================================
        List<String> anomalyInsights = anomalyResults.stream()
                .map(anomaly ->
                        anomaly.getType()
                                + " anomaly detected with severity "
                                + anomaly.getSeverity()
                )
                .toList();

        // =====================================================
        // ANALYSIS CONTEXT
        // =====================================================
        Analysis analysis = analysisRepository
                .findByUploadUploadIdAndUserId(
                        request.getUploadId(),
                        userId
                )
                .orElse(null);

        boolean analysisAvailable =
                analysis != null
                        && analysis.getAnalysisStatus()
                        == Analysis.AnalysisStatus.COMPLETED;

        // =====================================================
        // PROMPT BUILDING
        // =====================================================
        String prompt =
                promptBuilderService.buildChatPrompt(
                        request.getQuestion(),
                        logs
                );

        // =====================================================
        // STEP 10.10 - PROMPT COMPRESSION
        // =====================================================
        prompt = promptCompressionService.compress(prompt);

        // =====================================================
        // ROOT CAUSE ENRICHMENT
        // =====================================================
        prompt = prompt +
                "\n\nSYSTEM ROOT CAUSE DETECTION:\n" +
                "Detected Root Cause: " +
                rootCauseResult.getRootCause() +
                "\nConfidence Score: " +
                rootCauseResult.getConfidence() +
                "%";

        // =====================================================
        // AI CALL
        // =====================================================
        String answer = openAIClient.askQuestion(prompt);

        // =====================================================
        // SMART RETRY
        // =====================================================
        boolean weak =
                responseQualityService.isWeakResponse(answer);

        if (weak) {

            log.warn(
                    "Weak AI response detected. Retrying..."
            );

            String retryPrompt = prompt +
                    "\n\nIMPORTANT:\n" +
                    "Provide a more detailed technical analysis " +
                    "with root cause reasoning and fix suggestions.";

            answer = openAIClient.askQuestion(retryPrompt);
        }

        log.info(
                "AI response generated for uploadId={}",
                request.getUploadId()
        );

        answer = chatResponseFormatterService.format(answer);

        // =====================================================
        // CONFIDENCE SCORING
        // =====================================================
        int confidence = aiConfidenceService.calculateConfidence(
                logs,
                rootCauseResult,
                answer
        );

        String quality =
                aiConfidenceService.determineQuality(confidence);

        log.info(
                "AI confidence={} quality={}",
                confidence,
                quality
        );

        // =====================================================
        // GENERATE INSIGHTS
        // =====================================================
        List<String> insights =
                chatInsightService.generateInsights(
                        logs,
                        rootCauseResult,
                        confidence
                );

        insights.addAll(ruleFindings);
        insights.addAll(anomalyInsights);

        insights.add(
                "Prompt compression enabled"
        );

        insights.add(
                "Token optimization enabled"
        );

        // =====================================================
        // CACHE STORE
        // =====================================================
        chatCacheService.put(cacheKey, answer);

        log.info(
                "Chat response cached for uploadId={} userId={}",
                request.getUploadId(),
                userId
        );

        // =====================================================
        // USED LOGS
        // =====================================================
        List<UsedLogDto> usedLogs = logs.stream()
                .limit(10)
                .map(log -> UsedLogDto.builder()
                        .id(log.getId())
                        .timestamp(log.getLogTimestamp())
                        .level(log.getLevel().name())
                        .message(log.getMessage())
                        .build())
                .toList();

        // =====================================================
        // FINAL RESPONSE
        // =====================================================
        return ChatResponse.builder()
                .answer(answer)
                .usedLogs(usedLogs)
                .source("DIRECT")
                .confidence(confidence)
                .quality(quality)
                .insights(insights)
                .build();
    }

    public void validateUploadAccess(String uploadId, Long userId) {
        uploadRepository
                .findByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Upload not found")
                );
    }

    // =====================================================
    // CACHE KEY
    // =====================================================
    private String buildCacheKey(
            String uploadId,
            Long userId,
            String question
    ) {

        String normalized = question == null
                ? ""
                : question
                  .trim()
                  .toLowerCase()
                  .replaceAll("\\s+", " ");

        return uploadId +
                ":" +
                userId +
                ":" +
                normalized;
    }

    private Comparator<Log> timestampComparator() {
        return Comparator.comparing(
                Log::getLogTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }
}
