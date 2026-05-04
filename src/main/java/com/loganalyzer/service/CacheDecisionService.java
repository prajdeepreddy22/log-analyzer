package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheDecisionService {

    private final AnalysisRepository analysisRepository;

    public CacheDecision decide(String hash, Long userId) {

        log.info("Checking cache decision for hash={} userId={}", hash, userId);

        Optional<Analysis> existingOpt =
                analysisRepository.findByHashKeyAndUserId(hash, userId);

        if (existingOpt.isEmpty()) {
            log.info("No existing analysis found → NEW");
            return CacheDecision.NEW;
        }

        Analysis existing = existingOpt.get();

        log.info("Existing analysis found with status={}", existing.getAnalysisStatus());

        return switch (existing.getAnalysisStatus()) {

            case COMPLETED -> {
                log.info("Decision → SKIP (already completed)");
                yield CacheDecision.SKIP;
            }

            case FAILED -> {
                log.info("Decision → RETRY (previous attempt failed)");
                yield CacheDecision.RETRY;
            }

            case PROCESSING, PENDING -> {
                log.info("Decision → IN_PROGRESS (already running)");
                yield CacheDecision.IN_PROGRESS;
            }

            default -> {
                log.warn("Unexpected status → treating as NEW");
                yield CacheDecision.NEW;
            }
        };
    }

    public enum CacheDecision {
        NEW,
        SKIP,
        RETRY,
        IN_PROGRESS
    }
}