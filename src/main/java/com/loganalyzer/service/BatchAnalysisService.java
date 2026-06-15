package com.loganalyzer.service;

import com.loganalyzer.dto.batch.BatchAnalysisJob;
import com.loganalyzer.dto.response.AnalysisTriggerResponse;
import com.loganalyzer.exception.BadRequestException;
import com.loganalyzer.exception.ResourceNotFoundException;
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

        validateUploadIds(uploadIds);

        String batchId =
                UUID.randomUUID().toString();

        BatchAnalysisJob job =
                BatchAnalysisJob.builder()
                        .batchId(batchId)
                        .userId(userId)
                        .uploadIds(uploadIds)
                        .totalUploads(uploadIds.size())
                        .processedUploads(0)
                        .failedUploads(0)
                        .errors(new LinkedHashMap<>())
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
                        "Batch upload failed uploadId={} type={}",
                        uploadId,
                        e.getClass().getSimpleName()
                );

                job.setFailedUploads(job.getFailedUploads() + 1);
                job.getErrors().put(
                        uploadId,
                        safeBatchError(e)
                );
            }

            job.setProcessedUploads(
                    job.getProcessedUploads() + 1
            );
        }

        if (job.getFailedUploads() == 0) {
            job.setStatus("COMPLETED");
        } else if (job.getFailedUploads() == job.getTotalUploads()) {
            job.setStatus("FAILED");
        } else {
            job.setStatus("PARTIALLY_COMPLETED");
        }

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
            String batchId,
            Long userId
    ) {

        BatchAnalysisJob job =
                batchJobs.get(batchId);

        if (job == null) {

            throw new ResourceNotFoundException(
                    "Batch job not found"
            );
        }

        if (!job.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Batch job not found");
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

    private void validateUploadIds(List<String> uploadIds) {

        if (uploadIds == null || uploadIds.isEmpty()) {
            throw new BadRequestException("At least one upload ID is required");
        }

        if (uploadIds.size() > 100) {
            throw new BadRequestException(
                    "A batch cannot contain more than 100 uploads"
            );
        }

        if (uploadIds.stream().anyMatch(
                uploadId -> uploadId == null || uploadId.isBlank())) {
            throw new BadRequestException("Upload IDs cannot be blank");
        }
    }

    private String safeBatchError(Exception exception) {

        if (exception instanceof ResourceNotFoundException) {
            return "Upload not found";
        }

        if (exception instanceof BadRequestException) {
            return exception.getMessage();
        }

        return "Analysis could not be queued";
    }
}
