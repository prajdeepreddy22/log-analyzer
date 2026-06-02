package com.loganalyzer.service;

import com.loganalyzer.dto.ai.AIJobDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIWorkerService {

    private final AIQueueService aiQueueService;

    private final AIProcessingService aiProcessingService;

    @Qualifier("aiExecutor")
    private final Executor aiExecutor;

    // =========================================================
    // START WORKER ON APPLICATION STARTUP
    // =========================================================
    @PostConstruct
    public void init() {

        log.info("Initializing AI Worker");

        aiExecutor.execute(this::workerLoop);
    }

    // =========================================================
    // MAIN WORKER LOOP
    // =========================================================
    private void workerLoop() {

        log.info("AI Worker started successfully");

        while (!Thread.currentThread().isInterrupted()) {

            AIJobDto job = null;

            try {

                // =================================================
                // TAKE JOB FROM QUEUE
                // =================================================
                job = aiQueueService.take();

                log.info(
                        "Processing AI job uploadId={} hash={}",
                        job.getUploadId(),
                        job.getHash()
                );

                // =================================================
                // PROCESS AI ANALYSIS
                // =================================================
                aiProcessingService.processAnalysis(
                        job.getUploadId(),
                        job.getUserId(),
                        job.getHash(),
                        job.getLogs()
                );

                log.info(
                        "AI processing completed uploadId={} hash={}",
                        job.getUploadId(),
                        job.getHash()
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                log.error(
                        "AI Worker interrupted",
                        e
                );

                break;

            } catch (Exception e) {

                log.error(
                        "AI Worker processing failed",
                        e
                );

            } finally {

                // =============================================
                // ALWAYS RELEASE ACTIVE HASH
                // =============================================
                if (job != null) {

                    aiQueueService.markCompleted(
                            job.getHash()
                    );
                }
            }
        }
    }
}