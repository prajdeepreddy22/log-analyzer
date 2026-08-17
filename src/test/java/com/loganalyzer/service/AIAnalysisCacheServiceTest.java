package com.loganalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.dto.cache.CachedAnalysisResult;
import com.loganalyzer.entity.Analysis;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIAnalysisCacheServiceTest {

    @Test
    void storesCompletedAnalysisInRedis() {

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AIAnalysisCacheService service =
                new AIAnalysisCacheService(redisTemplate, new ObjectMapper());

        Analysis analysis = Analysis.builder()
                .analysisStatus(Analysis.AnalysisStatus.COMPLETED)
                .summary("summary")
                .rootCause("NULL_REFERENCE_ERROR")
                .severityScore((byte) 4)
                .confidenceScore(new BigDecimal("0.900"))
                .build();

        service.put("hash-1", analysis);

        verify(valueOperations).set(
                eq("ai:analysis:hash-1"),
                any(String.class),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    void skipsNonCompletedAnalysis() {

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        AIAnalysisCacheService service =
                new AIAnalysisCacheService(redisTemplate, new ObjectMapper());

        Analysis analysis = Analysis.builder()
                .analysisStatus(Analysis.AnalysisStatus.PROCESSING)
                .rootCause("NULL_REFERENCE_ERROR")
                .build();

        service.put("hash-1", analysis);

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void readsCachedAnalysisFromRedis() throws Exception {

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectMapper objectMapper = new ObjectMapper();

        CachedAnalysisResult cached = CachedAnalysisResult.builder()
                .summary("summary")
                .rootCause("NULL_REFERENCE_ERROR")
                .severityScore((byte) 4)
                .confidenceScore(new BigDecimal("0.900"))
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai:analysis:hash-1"))
                .thenReturn(objectMapper.writeValueAsString(cached));

        AIAnalysisCacheService service =
                new AIAnalysisCacheService(redisTemplate, objectMapper);

        Optional<CachedAnalysisResult> result = service.get("hash-1");

        assertThat(result).isPresent();
        assertThat(result.get().getRootCause())
                .isEqualTo("NULL_REFERENCE_ERROR");
    }
}
