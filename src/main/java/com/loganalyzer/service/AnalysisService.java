package com.loganalyzer.service;

import com.loganalyzer.dto.response.AnalysisResponse;
import com.loganalyzer.entity.*;
import com.loganalyzer.entity.Log.LogLevel;
import com.loganalyzer.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final LogRepository logRepository;
    private final UploadRepository uploadRepository;
    private final HashKeyService hashKeyService;
    private final AIProcessingService aiProcessingService;

    // Trigger analysis
    public void analyze(String uploadId, Long userId) {

        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload not found"));

        // Fetch ERROR + WARN logs
        List<Log> logs = logRepository
                .findTop100ByUploadUploadIdAndLevelInOrderByLogTimestampDesc(
                        uploadId,
                        List.of(LogLevel.ERROR, LogLevel.WARN)
                );

        if (logs.isEmpty()) {
            throw new RuntimeException("No logs to analyze");
        }

        // Generate hash
        String hash = hashKeyService.computeHashFromLogs(logs);

        // Check existing analysis
        var existing = analysisRepository.findByHashKeyAndUserId(hash, userId);

        if (existing.isPresent()) {
            Analysis existingAnalysis = existing.get();

            // Already completed → skip
            if (existingAnalysis.getAnalysisStatus() == Analysis.AnalysisStatus.COMPLETED) {
                return;
            }

            // If failed → allow retry
            if (existingAnalysis.getAnalysisStatus() == Analysis.AnalysisStatus.FAILED) {
                aiProcessingService.processAnalysis(uploadId, userId, hash, logs);
                return;
            }

            // If processing → do nothing
            return;
        }

        // Save initial record
        Analysis analysis = Analysis.builder()
                .upload(upload)
                .user(upload.getUser())
                .hashKey(hash)
                .analysisStatus(Analysis.AnalysisStatus.PENDING)
                .build();

        analysisRepository.save(analysis);

        // Trigger async AI processing
        aiProcessingService.processAnalysis(uploadId, userId, hash, logs);
    }

    // Fetch analysis result
    public AnalysisResponse getAnalysis(String uploadId, Long userId) {

        Analysis analysis = analysisRepository
                .findByUploadUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() -> new RuntimeException("Analysis not found"));

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
}