package com.loganalyzer.service;

import com.loganalyzer.dto.request.LiveIngestionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngestionDedupServiceTest {

    @Test
    void batchHashIncludesSourceId() {

        IngestionDedupService service =
                new IngestionDedupService(mock(StringRedisTemplate.class));

        LiveIngestionRequest firstSource = request(12L);
        LiveIngestionRequest secondSource = request(13L);

        String firstHash = service.computeBatchHash(1L, firstSource);
        String secondHash = service.computeBatchHash(1L, secondSource);

        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @Test
    void claimBatchUsesAtomicSetIfAbsentWithTtl() {

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock();
        IngestionDedupService service = new IngestionDedupService(redisTemplate);
        ReflectionTestUtils.setField(service, "dedupTtlMinutes", 5L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("ingest:batch:hash"),
                eq("processing"),
                eq(Duration.ofMinutes(5))
        )).thenReturn(true);

        boolean claimed = service.claimBatch("hash");

        assertThat(claimed).isTrue();
        verify(valueOperations).setIfAbsent(
                "ingest:batch:hash",
                "processing",
                Duration.ofMinutes(5)
        );
    }

    @Test
    void releaseBatchDeletesReservationKey() {

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        IngestionDedupService service = new IngestionDedupService(redisTemplate);

        service.releaseBatch("hash");

        verify(redisTemplate).delete("ingest:batch:hash");
    }

    private LiveIngestionRequest request(Long sourceId) {

        LiveIngestionRequest request = new LiveIngestionRequest();
        request.setSourceId(sourceId);
        request.setBatchTimestamp(Instant.parse("2026-08-18T13:55:00Z"));
        request.setLines(List.of(
                "2026-08-18 19:25:10 ERROR [AuthService] NullPointerException while processing login",
                "2026-08-18 19:25:12 WARN [PaymentService] Payment gateway timeout"
        ));
        return request;
    }
}
