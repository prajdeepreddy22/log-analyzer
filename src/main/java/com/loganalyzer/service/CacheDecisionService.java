package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheDecisionService {

    private final AnalysisRepository analysisRepository;

    public enum CacheDecision {
        SKIP,        // cached result exists — reuse it
        IN_PROGRESS, // AI call already running for this hash
        RETRY,       // previous attempt failed — retry
        NEW          // no record found — fresh AI call needed
    }

    public CacheDecision decide(String hash, Long userId) {

        // Find any analysis with this hash for this user
        // (from any previous upload — cross-upload cache check)
        return analysisRepository
                .findFirstByHashKeyAndUserIdOrderByUpdatedAtDesc(hash, userId)
                .map(analysis -> {

                    switch (analysis.getAnalysisStatus()) {

                        case COMPLETED:
                            return CacheDecision.SKIP;

                        case PROCESSING:
                            return CacheDecision.IN_PROGRESS;

                        case FAILED:
                        case RETRYING:
                            // Stop retry loop after 3 attempts
                            if (analysis.getRetryCount() >= 3) {
                                return CacheDecision.SKIP;
                            }
                            return CacheDecision.RETRY;

                        case PENDING:
                        default:
                            return CacheDecision.NEW;
                    }
                })
                .orElse(CacheDecision.NEW);
    }
}
