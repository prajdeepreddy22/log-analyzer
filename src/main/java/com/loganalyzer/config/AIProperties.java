package com.loganalyzer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIProperties {

    // =====================================================
    // TOKEN OPTIMIZATION
    // =====================================================
    private Integer maxLogs = 50;

    private Integer maxPromptLength = 12000;

    private Integer maxCompressedLogs = 25;

    private Integer maxMessageLength = 180;

    // =====================================================
    // STREAMING
    // =====================================================
    private Integer streamingDelayMs = 40;

    // =====================================================
    // RETRIES
    // =====================================================
    private Integer maxRetries = 3;

    // =====================================================
    // AI QUEUE
    // =====================================================
    private Integer queueCapacity = 500;

    // =====================================================
    // THREAD POOL
    // =====================================================
    private Integer corePoolSize = 4;

    private Integer maxPoolSize = 8;

    // =====================================================
    // CACHE
    // =====================================================
    private Integer cacheConfidence = 90;
}