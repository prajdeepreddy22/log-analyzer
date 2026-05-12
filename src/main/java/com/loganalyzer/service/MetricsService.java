package com.loganalyzer.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class MetricsService {

    // =========================================================
    // METER REGISTRY
    // =========================================================
    private final MeterRegistry meterRegistry;

    // =========================================================
    // COUNTERS
    // =========================================================
    private final Counter uploadCounter;

    private final Counter aiRequestCounter;

    private final Counter aiSuccessCounter;

    private final Counter aiFailureCounter;

    private final Counter cacheHitCounter;

    private final Counter rateLimitCounter;

    // =========================================================
    // TIMERS
    // =========================================================
    private final Timer aiProcessingTimer;

    private final Timer uploadProcessingTimer;

    public MetricsService(MeterRegistry registry) {

        this.meterRegistry = registry;

        // =====================================================
        // UPLOADS
        // =====================================================
        this.uploadCounter = Counter.builder(
                        "loganalyzer.upload.count")
                .description("Total uploads")
                .register(registry);

        // =====================================================
        // AI REQUESTS
        // =====================================================
        this.aiRequestCounter = Counter.builder(
                        "loganalyzer.ai.requests")
                .description("Total AI requests")
                .register(registry);

        // =====================================================
        // AI SUCCESS
        // =====================================================
        this.aiSuccessCounter = Counter.builder(
                        "loganalyzer.ai.success")
                .description("Total successful AI analyses")
                .register(registry);

        // =====================================================
        // AI FAILURES
        // =====================================================
        this.aiFailureCounter = Counter.builder(
                        "loganalyzer.ai.failures")
                .description("Total AI failures")
                .register(registry);

        // =====================================================
        // CACHE HITS
        // =====================================================
        this.cacheHitCounter = Counter.builder(
                        "loganalyzer.cache.hits")
                .description("Total cache hits")
                .register(registry);

        // =====================================================
        // RATE LIMIT
        // =====================================================
        this.rateLimitCounter = Counter.builder(
                        "loganalyzer.rate.limit.exceeded")
                .description("Rate limit exceeded count")
                .register(registry);

        // =====================================================
        // AI TIMER
        // =====================================================
        this.aiProcessingTimer = Timer.builder(
                        "loganalyzer.ai.processing.time")
                .description("AI processing duration")
                .register(registry);

        // =====================================================
        // UPLOAD TIMER
        // =====================================================
        this.uploadProcessingTimer = Timer.builder(
                        "loganalyzer.upload.processing.time")
                .description("Upload processing duration")
                .register(registry);
    }
}