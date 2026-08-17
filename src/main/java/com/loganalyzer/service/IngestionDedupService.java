package com.loganalyzer.service;

import com.loganalyzer.dto.request.LiveIngestionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class IngestionDedupService {

    private static final String KEY_PREFIX = "ingest:batch:";

    private final StringRedisTemplate redisTemplate;

    @Value("${aeip.ingest.dedup-ttl-minutes:5}")
    private long dedupTtlMinutes;

    public String computeBatchHash(
            Long userId,
            LiveIngestionRequest request
    ) {

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(String.valueOf(userId));
        joiner.add(String.valueOf(request.getSourceId()));
        joiner.add(String.valueOf(request.getBatchTimestamp()));
        request.getLines().forEach(joiner::add);

        return sha256(joiner.toString());
    }

    public boolean isDuplicate(String batchHash) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(batchHash)));
    }

    public void markProcessed(String batchHash) {
        redisTemplate.opsForValue().set(
                key(batchHash),
                "processed",
                Duration.ofMinutes(dedupTtlMinutes)
        );
    }

    private String key(String batchHash) {
        return KEY_PREFIX + batchHash;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
