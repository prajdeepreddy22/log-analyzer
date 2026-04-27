package com.loganalyzer.service;

import com.loganalyzer.client.OpenAIClient;
import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import com.loganalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIProcessingService {

    private final AnalysisRepository analysisRepository;
    private final PromptBuilderService promptBuilder;
    private final OpenAIClient openAIClient;

    @Async
    public void processAnalysis(String uploadId, Long userId, String hash, List<Log> logs) {

        try {
            //Update status → PROCESSING (FIXED: using hash)
            analysisRepository.updateStatusByHash(
                    hash,
                    userId,
                    Analysis.AnalysisStatus.PROCESSING
            );

            String prompt = promptBuilder.buildPrompt(logs);

            Map<String, Object> response = openAIClient.analyzeLogs(prompt);

            Analysis analysis = analysisRepository
                    .findByHashKeyAndUserId(hash, userId)
                    .orElseThrow();

            //Save result
            analysis.setSummary((String) response.get("summary"));
            analysis.setRootCause((String) response.get("root_cause"));
            analysis.setDeveloperMistake((String) response.get("developer_mistake"));
            analysis.setFixSuggestion((String) response.get("fix_suggestion"));
            analysis.setCodeFix((String) response.get("code_fix"));
            analysis.setSeverityScore((Integer) response.get("severity_score"));
            analysis.setAnalysisStatus(Analysis.AnalysisStatus.COMPLETED);

            analysisRepository.save(analysis);

        } catch (Exception e) {

            // FIXED: retry using hash
            analysisRepository.updateStatusAndRetryByHash(
                    hash,
                    userId,
                    Analysis.AnalysisStatus.FAILED,
                    e.getMessage()
            );
        }
    }
}