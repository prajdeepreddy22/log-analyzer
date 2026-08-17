package com.loganalyzer.service;

import com.loganalyzer.client.OpenAIClient;
import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import com.loganalyzer.event.AnalysisCompletedEvent;
import com.loganalyzer.event.AnalysisStartedEvent;
import com.loganalyzer.exception.AIProviderException;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.AnalysisRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIProcessingService {

    @Value("${app.ai.max-retries:2}")
    private int maxRetries;

    private final AnalysisRepository analysisRepository;
    private final AnalysisPersistenceService analysisPersistenceService;

    // CHANGED
    private final PromptBuilderService promptBuilderService;

    private final OpenAIClient openAIClient;
    private final RootCauseDetectorService rootCauseDetectorService;
    private final ConfidenceScoreService confidenceScoreService;
    private final MetricsService metricsService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================
    // MAIN AI PROCESSING
    // =========================================================
    public void processAnalysis(
            String uploadId,
            Long userId,
            String hash,
            List<Log> logs
    ) {

        int attempt = 0;

        while (attempt < maxRetries) {

            Timer.Sample sample =
                    Timer.start(
                            metricsService.getMeterRegistry()
                    );

            try {

                attempt++;

                // =====================================================
                // AI REQUEST METRIC
                // =====================================================
                metricsService
                        .getAiRequestCounter()
                        .increment();

                log.info(
                        "AI START uploadId={} hash={} attempt={}",
                        uploadId,
                        hash,
                        attempt
                );

                Analysis analysis = analysisRepository
                        .findByUploadUploadIdAndUserId(uploadId, userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Analysis record not found for uploadId="
                                                + uploadId
                                )
                        );

                analysis.setAnalysisStatus(
                        Analysis.AnalysisStatus.PROCESSING
                );

                analysisPersistenceService.save(analysis);

                eventPublisher.publishEvent(
                        new AnalysisStartedEvent(
                                userId,
                                analysis.getId(),
                                uploadId
                        )
                );

                // =====================================================
                // BUILD PROMPT
                // =====================================================
                String prompt =
                        promptBuilderService
                                .buildAnalysisPrompt(logs);

                log.info(
                        "Prompt generated uploadId={} promptLength={}",
                        uploadId,
                        prompt.length()
                );

                // =====================================================
                // CALL OPENAI
                // =====================================================
                Map<String, Object> response =
                        openAIClient.analyzeLogs(prompt);

                log.info(
                        "AI response received uploadId={}",
                        uploadId
                );

                // =====================================================
                // MAP RESPONSE
                // =====================================================
                analysis.setSummary(
                        getValue(response, "summary")
                );

                String aiRootCause = getValue(
                        response,
                        "rootCause",
                        "root_cause"
                );

                String finalRootCause =
                        rootCauseDetectorService.detect(
                                logs,
                                aiRootCause
                        );

                analysis.setRootCause(finalRootCause);

                BigDecimal confidenceScore =
                        confidenceScoreService.calculate(
                                logs,
                                finalRootCause
                        );

                analysis.setConfidenceScore(confidenceScore);

                log.info(
                        "Analysis confidence rootCause={} confidenceScore={}",
                        finalRootCause,
                        confidenceScore
                );

                analysis.setDeveloperMistake(
                        getValue(
                                response,
                                "developerMistake",
                                "developer_mistake"
                        )
                );

                analysis.setFixSuggestion(
                        getValue(
                                response,
                                "fixSuggestion",
                                "fix_suggestion"
                        )
                );

                analysis.setCodeFix(
                        getValue(
                                response,
                                "codeFix",
                                "code_fix"
                        )
                );

                analysis.setSeverityScore(
                        getSeverityScore(
                                response,
                                "severityScore",
                                "severity_score"
                        )
                );

                // =====================================================
                // SUCCESS
                // =====================================================
                analysis.setRetryCount(attempt - 1);

                analysis.setAnalysisStatus(
                        Analysis.AnalysisStatus.COMPLETED
                );

                analysis.setErrorMessage(null);

                analysisPersistenceService.save(analysis);

                eventPublisher.publishEvent(
                        new AnalysisCompletedEvent(
                                userId,
                                analysis.getId(),
                                uploadId,
                                analysis.getAnalysisStatus().name(),
                                analysis.getConfidenceScore() == null
                                        ? "0.000"
                                        : analysis.getConfidenceScore().toPlainString()
                        )
                );

                // =====================================================
                // SUCCESS METRIC
                // =====================================================
                metricsService
                        .getAiSuccessCounter()
                        .increment();

                // =====================================================
                // TIMER STOP
                // =====================================================
                sample.stop(
                        metricsService
                                .getAiProcessingTimer()
                );

                log.info(
                        "AI COMPLETED uploadId={} hash={} attempt={}",
                        uploadId,
                        hash,
                        attempt
                );

                return;

            } catch (Exception e) {

                // =====================================================
                // FAILURE METRIC
                // =====================================================
                metricsService
                        .getAiFailureCounter()
                        .increment();

                log.error(
                        "AI FAILED uploadId={} hash={} attempt={} type={}",
                        uploadId,
                        hash,
                        attempt,
                        e.getClass().getSimpleName()
                );

                boolean retryable = isRetryable(e);

                if (!retryable || attempt >= maxRetries) {

                    analysisRepository
                            .updateStatusAndRetryByUploadId(
                                    uploadId,
                                    userId,
                                    Analysis.AnalysisStatus.FAILED,
                                    safeError(e)
                            );

                    log.error(
                            "AI permanently FAILED uploadId={} after {} attempts retryable={}",
                            uploadId,
                            attempt,
                            retryable
                    );

                    return;
                }

                analysisRepository
                        .updateStatusAndRetryByUploadId(
                                uploadId,
                                userId,
                                Analysis.AnalysisStatus.RETRYING,
                                safeError(e)
                        );

                log.warn(
                        "Retrying AI uploadId={} nextAttempt={}",
                        uploadId,
                        attempt + 1
                );

                // =====================================================
                // RETRY DELAY
                // =====================================================
                try {

                    Thread.sleep(attempt * 2000L);

                } catch (InterruptedException ex) {

                    Thread.currentThread().interrupt();

                    analysisRepository
                            .updateStatusAndRetryByUploadId(
                                    uploadId,
                                    userId,
                                    Analysis.AnalysisStatus.FAILED,
                                    "Analysis interrupted during retry"
                            );

                    log.warn(
                            "Retry sleep interrupted uploadId={}",
                            uploadId
                    );

                    return;
                }
            }
        }
    }

    // =========================================================
    // HELPER — STRING VALUE
    // =========================================================
    private String getValue(
            Map<String, Object> map,
            String... keys
    ) {

        for (String key : keys) {

            Object value = map.get(key);

            if (value != null) {
                return value.toString();
            }
        }

        return "N/A";
    }

    // =========================================================
    // HELPER — INTEGER VALUE
    // =========================================================
    private Byte getSeverityScore(
            Map<String, Object> map,
            String... keys
    ) {

        for (String key : keys) {

            Object value = map.get(key);

            if (value != null) {

                try {

                    int score = Integer.parseInt(
                            value.toString()
                    );

                    return SeverityScoreMapper.toEntityValue(score);

                } catch (Exception ignored) {
                }
            }
        }

        return SeverityScoreMapper.toEntityValue(1);
    }

    // =========================================================
    // SAFE ERROR MESSAGE
    // =========================================================
    private String safeError(Exception e) {

        if (e instanceof AIProviderException providerException) {
            Integer status = providerException.getProviderStatus();

            if (status == null) {
                String message = providerException.getMessage();
                return message != null && message.toLowerCase().contains("json")
                        ? "AI service returned an invalid response"
                        : "AI service is unavailable";
            }

            if (status == 401 || status == 403) {
                return "AI service authentication failed";
            }

            if (status == 408) {
                return "AI service request timed out";
            }

            if (status == 429) {
                return "AI service rate limit exceeded";
            }

            if (status >= 500) {
                return "AI service is temporarily unavailable";
            }

            return "AI service could not process the request";
        }

        return "Analysis processing failed";
    }

    private boolean isRetryable(Exception e) {
        return !(e instanceof AIProviderException providerException)
                || providerException.isRetryable();
    }
}
