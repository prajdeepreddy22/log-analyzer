package com.loganalyzer.service;

import com.loganalyzer.dto.batch.BatchAnalysisJob;
import com.loganalyzer.dto.response.AnalysisTriggerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchAnalysisService {

    private final AnalysisService analysisService;

    // =====================================================
    // IN MEMORY BATCH STORE
    // =====================================================
    private final Map<String, BatchAnalysisJob> batchJobs =
            new ConcurrentHashMap<>();

    // =====================================================
    // START BATCH ANALYSIS
    // =====================================================
    public BatchAnalysisJob startBatchAnalysis(
            List<String> uploadIds,
            Long userId
    ) {

        String batchId =
                UUID.randomUUID().toString();

        BatchAnalysisJob job =
                BatchAnalysisJob.builder()
                        .batchId(batchId)
                        .userId(userId)
                        .uploadIds(uploadIds)
                        .totalUploads(uploadIds.size())
                        .processedUploads(0)
                        .status("PROCESSING")
                        .createdAt(LocalDateTime.now())
                        .build();

        batchJobs.put(batchId, job);

        log.info(
                "Batch analysis started batchId={} uploads={}",
                batchId,
                uploadIds.size()
        );

        // =====================================================
        // PROCESS EACH UPLOAD
        // =====================================================
        for (String uploadId : uploadIds) {

            try {

                AnalysisTriggerResponse response =
                        analysisService.analyze(
                                uploadId,
                                userId,
                                false
                        );

                log.info(
                        "Batch upload queued uploadId={} status={}",
                        uploadId,
                        response.getStatus()
                );

            } catch (Exception e) {

                log.error(
                        "Batch upload failed uploadId={}",
                        uploadId,
                        e
                );
            }

            job.setProcessedUploads(
                    job.getProcessedUploads() + 1
            );
        }

        job.setStatus("COMPLETED");

        log.info(
                "Batch analysis completed batchId={}",
                batchId
        );

        return job;
    }

    // =====================================================
    // GET BATCH STATUS
    // =====================================================
    public BatchAnalysisJob getBatchStatus(
            String batchId
    ) {

        BatchAnalysisJob job =
                batchJobs.get(batchId);

        if (job == null) {

            throw new RuntimeException(
                    "Batch job not found"
            );
        }

        return job;
    }

    // =====================================================
    // GET ALL BATCHES FOR THIS USER ONLY
    // =====================================================
    public List<BatchAnalysisJob> getAllJobsByUser(Long userId) {
        return batchJobs.values()
                .stream()
                .filter(job -> job.getUserId().equals(userId))
                .sorted(Comparator.comparing(BatchAnalysisJob::getCreatedAt).reversed())
                .toList();
    }
}