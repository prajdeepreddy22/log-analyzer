package com.loganalyzer.service;

import com.loganalyzer.client.OpenAIClient;
import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import com.loganalyzer.repository.AnalysisRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIProcessingService {

    @Value("${app.ai.max-retries:2}")
    private int maxRetries;

    private final AnalysisRepository analysisRepository;

    // CHANGED
    private final PromptBuilderService promptBuilderService;

    private final OpenAIClient openAIClient;
    private final RootCauseDetectorService rootCauseDetectorService;
    private final MetricsService metricsService;

    // =========================================================
    // MAIN AI PROCESSING
    // =========================================================
    @Transactional
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
                                new RuntimeException(
                                        "Analysis record not found for uploadId="
                                                + uploadId
                                )
                        );

                analysis.setAnalysisStatus(
                        Analysis.AnalysisStatus.PROCESSING
                );

                analysisRepository.save(analysis);

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

                analysisRepository.save(analysis);

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
                        "AI FAILED uploadId={} hash={} attempt={}",
                        uploadId,
                        hash,
                        attempt,
                        e
                );

                if (attempt >= maxRetries) {

                    analysisRepository
                            .updateStatusAndRetryByUploadId(
                                    uploadId,
                                    userId,
                                    Analysis.AnalysisStatus.FAILED,
                                    safeError(e)
                            );

                    log.error(
                            "AI permanently FAILED uploadId={} after {} attempts",
                            uploadId,
                            attempt
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

                    log.error(
                            "Retry sleep interrupted uploadId={}",
                            uploadId
                    );
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
    private Integer getSeverityScore(
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

                    return Math.max(1, Math.min(5, score));

                } catch (Exception ignored) {
                }
            }
        }

        return 1;
    }

    // =========================================================
    // SAFE ERROR MESSAGE
    // =========================================================
    private String safeError(Exception e) {

        if (e.getMessage() == null) {
            return e.getClass().getSimpleName();
        }

        return e.getMessage().length() > 500
                ? e.getMessage().substring(0, 500)
                : e.getMessage();
    }
}
