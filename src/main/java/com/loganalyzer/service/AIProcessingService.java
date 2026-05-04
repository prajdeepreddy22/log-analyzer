package com.loganalyzer.service;

import com.loganalyzer.client.OpenAIClient;
import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import com.loganalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIProcessingService {

    private final AnalysisRepository analysisRepository;
    private final PromptBuilderService promptBuilder;
    private final OpenAIClient openAIClient;

    @Async
    public void processAnalysis(String uploadId, Long userId, String hash, List<Log> logs) {

        try {
            log.info("Starting AI analysis | uploadId={} userId={} hash={}", uploadId, userId, hash);

            // STEP 1: mark processing
            analysisRepository.updateStatusByHash(
                    hash,
                    userId,
                    Analysis.AnalysisStatus.PROCESSING
            );

            // STEP 2: build prompt
            String prompt = promptBuilder.buildPrompt(logs);

            // STEP 3: call AI
            Map<String, Object> response = openAIClient.analyzeLogs(prompt);

            log.info("AI RESPONSE = {}", response);

            // STEP 4: fetch DB row
            Analysis analysis = analysisRepository
                    .findByHashKeyAndUserId(hash, userId)
                    .orElseThrow(() -> new RuntimeException("Analysis not found for hash"));

            // STEP 5: map safely (supports BOTH formats)
            analysis.setSummary(getValue(response, "summary"));
            analysis.setRootCause(getValue(response, "rootCause", "root_cause"));
            analysis.setDeveloperMistake(getValue(response, "developerMistake", "developer_mistake"));
            analysis.setFixSuggestion(getValue(response, "fixSuggestion", "fix_suggestion"));
            analysis.setCodeFix(getValue(response, "codeFix", "code_fix"));
            analysis.setSeverityScore(getIntValue(response, "severityScore", "severity_score"));

            // STEP 6: mark completed
            analysis.setAnalysisStatus(Analysis.AnalysisStatus.COMPLETED);

            analysisRepository.save(analysis);

            log.info("AI analysis COMPLETED for hash={}", hash);

        } catch (Exception e) {

            log.error("AI processing FAILED for hash={} error={}", hash, e.getMessage(), e);

            analysisRepository.updateStatusAndRetryByHash(
                    hash,
                    userId,
                    Analysis.AnalysisStatus.FAILED,
                    e.getMessage()
            );
        }
    }

    // =========================
    // 🔥 FLEXIBLE KEY FETCH
    // =========================
    private String getValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return "N/A";
    }

    private Integer getIntValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                try {
                    return Integer.parseInt(value.toString());
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }
}