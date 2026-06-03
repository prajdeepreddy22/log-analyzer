package com.loganalyzer.service;

import com.loganalyzer.dto.response.RateLimitStatus;
import com.loganalyzer.entity.RateLimitUsage;
import com.loganalyzer.repository.RateLimitUsageRepository;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    @Test
    void dailyUsageShouldRemainAfterMinuteWindowExpires() {

        RateLimitService service =
                rateLimitService(rateLimitUsageRepository());

        service.checkLimit(1L);

        Map<Long, List<LocalDateTime>> minuteWindow =
                (Map<Long, List<LocalDateTime>>) ReflectionTestUtils.getField(
                        service,
                        "minuteWindow"
                );

        minuteWindow.put(
                1L,
                Collections.synchronizedList(
                        new ArrayList<>(
                                List.of(LocalDateTime.now().minusMinutes(2))
                        )
                )
        );

        RateLimitStatus status = service.getStatus(1L);

        assertThat(status.getMinuteUsage()).isZero();
        assertThat(status.getDailyUsage()).isEqualTo(1);
        assertThat(status.getUsed()).isEqualTo(1);
        assertThat(status.getMinuteResetInSeconds()).isZero();
        assertThat(status.getDailyResetInSeconds()).isPositive();
    }

    @Test
    void minuteResetShouldRemainPositiveDuringActiveWindow() {

        RateLimitService service =
                rateLimitService(rateLimitUsageRepository());

        service.checkLimit(1L);

        RateLimitStatus status = service.getStatus(1L);

        assertThat(status.getMinuteUsage()).isEqualTo(1);
        assertThat(status.getMinuteResetInSeconds())
                .isBetween(1L, 60L);
        assertThat(status.getMinuteResetTimeFormatted())
                .matches("00H (00|01)M [0-5][0-9]s|00H 01M 00s");
    }

    @Test
    void dailyUsageShouldSurviveNewServiceInstance() {

        RateLimitUsageRepository rateLimitUsageRepository =
                rateLimitUsageRepository();

        RateLimitService firstService =
                rateLimitService(rateLimitUsageRepository);

        firstService.checkLimit(1L);

        RateLimitService restartedService =
                rateLimitService(rateLimitUsageRepository);

        RateLimitStatus status = restartedService.getStatus(1L);

        assertThat(status.getMinuteUsage()).isZero();
        assertThat(status.getDailyUsage()).isEqualTo(1);
        assertThat(status.getDailyRemaining()).isEqualTo(99);
    }

    private RateLimitService rateLimitService(
            RateLimitUsageRepository rateLimitUsageRepository) {

        MetricsService metricsService = mock(MetricsService.class);
        when(metricsService.getRateLimitCounter()).thenReturn(mock(Counter.class));

        RateLimitService service = new RateLimitService(
                metricsService,
                rateLimitUsageRepository);
        ReflectionTestUtils.setField(service, "maxRequestsPerMinute", 5);
        ReflectionTestUtils.setField(service, "windowMinutes", 1);
        ReflectionTestUtils.setField(service, "dailyLimit", 100);
        return service;
    }

    private RateLimitUsageRepository rateLimitUsageRepository() {

        RateLimitUsageRepository repository =
                mock(RateLimitUsageRepository.class);
        Map<String, RateLimitUsage> storage = new HashMap<>();
        AtomicLong ids = new AtomicLong(1);

        when(repository.findByUserIdAndUsageDate(any(), any()))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    LocalDate usageDate = invocation.getArgument(1);
                    return Optional.ofNullable(
                            storage.get(key(userId, usageDate)));
                });

        when(repository.save(any(RateLimitUsage.class)))
                .thenAnswer(invocation -> {
                    RateLimitUsage usage = invocation.getArgument(0);
                    if (usage.getId() == null) {
                        usage.setId(ids.getAndIncrement());
                    }
                    storage.put(
                            key(usage.getUserId(), usage.getUsageDate()),
                            usage);
                    return usage;
                });

        return repository;
    }

    private String key(
            Long userId,
            LocalDate usageDate) {

        return userId + ":" + usageDate;
    }
}
