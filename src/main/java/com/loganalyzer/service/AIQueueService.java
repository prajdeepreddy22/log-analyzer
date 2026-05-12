package com.loganalyzer.service;

import com.loganalyzer.dto.ai.AIJobDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIQueueService {

    // =========================================================
    // IN-MEMORY QUEUE
    // =========================================================
    private final BlockingQueue<AIJobDto> queue =
            new LinkedBlockingQueue<>();

    // =========================================================
    // ACTIVE HASHES
    // Prevent duplicate processing
    // =========================================================
    private final Set<String> activeHashes =
            ConcurrentHashMap.newKeySet();

    // =========================================================
    // ENQUEUE JOB
    // =========================================================
    public boolean enqueue(AIJobDto job) {

        // =====================================================
        // DUPLICATE CHECK
        // =====================================================
        if (activeHashes.contains(job.getHash())) {

            log.warn(
                    "Duplicate AI job skipped hash={} uploadId={}",
                    job.getHash(),
                    job.getUploadId()
            );

            return false;
        }

        activeHashes.add(job.getHash());

        queue.offer(job);

        log.info(
                "AI job queued uploadId={} hash={} queueSize={}",
                job.getUploadId(),
                job.getHash(),
                queue.size()
        );

        return true;
    }

    // =========================================================
    // TAKE JOB
    // =========================================================
    public AIJobDto take() throws InterruptedException {

        AIJobDto job = queue.take();

        log.info(
                "AI job dequeued uploadId={} remainingQueue={}",
                job.getUploadId(),
                queue.size()
        );

        return job;
    }

    // =========================================================
    // MARK COMPLETED
    // =========================================================
    public void markCompleted(String hash) {

        activeHashes.remove(hash);

        log.info(
                "AI job completed hash={}",
                hash
        );
    }

    // =========================================================
    // CHECK ACTIVE
    // =========================================================
    public boolean isActive(String hash) {
        return activeHashes.contains(hash);
    }

    // =========================================================
    // QUEUE SIZE
    // =========================================================
    public int size() {
        return queue.size();
    }
}