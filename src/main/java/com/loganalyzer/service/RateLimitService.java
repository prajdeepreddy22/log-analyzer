package com.loganalyzer.service;

import com.loganalyzer.dto.response.RateLimitStatus;
import com.loganalyzer.entity.RateLimitUsage;
import com.loganalyzer.exception.RateLimitExceededException;
import com.loganalyzer.repository.RateLimitUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    @Value("${app.rate-limit.max-requests:5}")
    private int maxRequestsPerMinute;

    @Value("${app.rate-limit.window-minutes:1}")
    private int windowMinutes;

    @Value("${app.rate-limit.daily-limit:100}")
    private int dailyLimit;

    private final MetricsService metricsService;
    private final RateLimitUsageRepository rateLimitUsageRepository;

    // =====================================================
    // userId -> timestamps
    // =====================================================

    private final Map<Long, List<LocalDateTime>> minuteWindow =
            new ConcurrentHashMap<>();

    // =====================================================
    // MAIN RATE LIMIT CHECK
    // =====================================================

    @Transactional
    public void checkLimit(Long userId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        cleanupOldTimestamps(userId, now);

        List<LocalDateTime> timestamps =
                minuteWindow.computeIfAbsent(
                        userId,
                        k -> Collections.synchronizedList(
                                new ArrayList<>()
                        )
                );

        synchronized (timestamps) {

            // =================================================
            // MINUTE LIMIT
            // =================================================

            if (timestamps.size() >= maxRequestsPerMinute) {

                metricsService
                        .getRateLimitCounter()
                        .increment();

                log.warn(
                        "Minute rate limit exceeded userId={} count={}",
                        userId,
                        timestamps.size()
                );

                throw new RateLimitExceededException(
                        "Rate limit exceeded. Max "
                                + maxRequestsPerMinute
                                + " AI requests per minute."
                );
            }

            int todayCount =
                    getDailyCount(userId, today);

            // =================================================
            // DAILY LIMIT
            // =================================================

            if (todayCount >= dailyLimit) {

                metricsService
                        .getRateLimitCounter()
                        .increment();

                log.warn(
                        "Daily rate limit exceeded userId={} count={}",
                        userId,
                        todayCount
                );

                throw new RateLimitExceededException(
                        "Daily AI limit reached. Max "
                                + dailyLimit
                                + " requests per day."
                );
            }

            // =================================================
            // RECORD REQUEST
            // =================================================

            timestamps.add(now);

            incrementDailyCount(userId, today);

            log.debug(
                    "Rate limit updated userId={} minuteCount={} dailyCount={}",
                    userId,
                    timestamps.size(),
                    todayCount + 1
            );
        }
    }

    // =====================================================
    // STATUS API SUPPORT
    // =====================================================

    @Transactional(readOnly = true)
    public RateLimitStatus getStatus(Long userId) {

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        cleanupOldTimestamps(userId, now);

        List<LocalDateTime> timestamps =
                minuteWindow.getOrDefault(
                        userId,
                        Collections.emptyList()
                );

        int minuteUsage =
                timestamps.size();

        int dailyUsage =
                getDailyCount(userId, today);

        long resetInSeconds = 0;

        if (!timestamps.isEmpty()) {

            LocalDateTime oldest =
                    timestamps.get(0);

            LocalDateTime expiry =
                    oldest.plusMinutes(windowMinutes);

            resetInSeconds =
                    secondsUntil(now, expiry);
        }

        long dailyResetInSeconds =
                secondsUntil(
                        now,
                        today
                                .plusDays(1)
                                .atStartOfDay()
                );

        return RateLimitStatus.builder()
                .userId(userId)
                .minuteUsage(minuteUsage)
                .minuteLimit(maxRequestsPerMinute)
                .dailyUsage(dailyUsage)
                .dailyLimit(dailyLimit)
                .resetInSeconds(resetInSeconds)
                .minuteResetInSeconds(resetInSeconds)
                .dailyResetInSeconds(Math.max(0, dailyResetInSeconds))
                .blocked(
                        minuteUsage >= maxRequestsPerMinute
                                || dailyUsage >= dailyLimit
                )
                .build();
    }

    // =====================================================
    // CLEANUP
    // =====================================================

    private void cleanupOldTimestamps(Long userId) {
        cleanupOldTimestamps(userId, LocalDateTime.now());
    }

    private void cleanupOldTimestamps(Long userId, LocalDateTime now) {

        LocalDateTime cutoff =
                now.minusMinutes(windowMinutes);

        minuteWindow.computeIfPresent(
                userId,
                (k, timestamps) -> {

                    synchronized (timestamps) {

                        timestamps.removeIf(
                                ts -> !ts.isAfter(cutoff)
                        );

                        return timestamps;
                    }
                }
        );
    }

    private long secondsUntil(
            LocalDateTime now,
            LocalDateTime target) {

        long millis =
                Duration.between(now, target)
                        .toMillis();

        if (millis <= 0) {
            return 0;
        }

        return (long) Math.ceil(millis / 1000.0);
    }

    // =====================================================
    // DAILY COUNT
    // =====================================================

    private int getDailyCount(
            Long userId,
            LocalDate today) {

        return rateLimitUsageRepository
                .findByUserIdAndUsageDate(userId, today)
                .map(RateLimitUsage::getUsageCount)
                .orElse(0);
    }

    private void incrementDailyCount(
            Long userId,
            LocalDate today) {

        RateLimitUsage usage =
                rateLimitUsageRepository
                        .findByUserIdAndUsageDate(userId, today)
                        .orElseGet(() ->
                                RateLimitUsage.builder()
                                        .userId(userId)
                                        .usageDate(today)
                                        .usageCount(0)
                                        .build()
                        );

        usage.setUsageCount(usage.getUsageCount() + 1);

        rateLimitUsageRepository.save(usage);
    }
}
