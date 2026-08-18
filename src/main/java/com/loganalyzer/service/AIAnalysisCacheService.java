package com.loganalyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.dto.cache.CachedAnalysisResult;
import com.loganalyzer.entity.Analysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAnalysisCacheService {

    private static final String KEY_PREFIX = "ai:analysis:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${aeip.cache.analysis-ttl-hours:24}")
    private long ttlHours = 24;

    public Optional<CachedAnalysisResult> get(String hash) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(key(hash));

            if (cachedJson == null || cachedJson.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(
                    objectMapper.readValue(
                            cachedJson,
                            CachedAnalysisResult.class
                    )
            );
        } catch (JsonProcessingException ex) {
            log.warn("Invalid cached analysis payload hash={}", hash);
            redisTemplate.delete(key(hash));
            return Optional.empty();
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            log.warn(
                    "Redis unavailable while reading analysis cache hash={}",
                    hash
            );
            return Optional.empty();
        }
    }

    public void put(
            String hash,
            Analysis analysis
    ) {
        if (!isCacheable(analysis)) {
            return;
        }

        try {
            CachedAnalysisResult payload = CachedAnalysisResult.builder()
                    .summary(analysis.getSummary())
                    .rootCause(analysis.getRootCause())
                    .developerMistake(analysis.getDeveloperMistake())
                    .fixSuggestion(analysis.getFixSuggestion())
                    .codeFix(analysis.getCodeFix())
                    .severityScore(analysis.getSeverityScore())
                    .confidenceScore(
                            ConfidenceScoreMapper.toEntityValue(
                                    analysis.getConfidenceScore()
                            )
                    )
                    .build();

            redisTemplate.opsForValue().set(
                    key(hash),
                    objectMapper.writeValueAsString(payload),
                    Duration.ofHours(ttlHours)
            );
        } catch (JsonProcessingException ex) {
            log.warn("Unable to serialize analysis cache hash={}", hash);
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            log.warn(
                    "Redis unavailable while writing analysis cache hash={}",
                    hash
            );
        }
    }

    public void applyToAnalysis(
            CachedAnalysisResult cached,
            Analysis analysis
    ) {
        analysis.setSummary(cached.getSummary());
        analysis.setRootCause(cached.getRootCause());
        analysis.setDeveloperMistake(cached.getDeveloperMistake());
        analysis.setFixSuggestion(cached.getFixSuggestion());
        analysis.setCodeFix(cached.getCodeFix());
        analysis.setSeverityScore(
                SeverityScoreMapper.toEntityValue(cached.getSeverityScore())
        );
        analysis.setConfidenceScore(
                ConfidenceScoreMapper.toEntityValue(cached.getConfidenceScore())
        );
    }

    private boolean isCacheable(Analysis analysis) {
        return analysis != null
                && analysis.getAnalysisStatus() == Analysis.AnalysisStatus.COMPLETED
                && analysis.getRootCause() != null
                && !analysis.getRootCause().isBlank();
    }

    private String key(String hash) {
        return KEY_PREFIX + hash;
    }
}
