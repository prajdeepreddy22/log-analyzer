package com.loganalyzer.service;

import com.loganalyzer.dto.response.RateLimitStatus;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    @Test
    void dailyUsageShouldRemainAfterMinuteWindowExpires() {

        MetricsService metricsService = mock(MetricsService.class);
        when(metricsService.getRateLimitCounter()).thenReturn(mock(Counter.class));

        RateLimitService service = new RateLimitService(metricsService);
        ReflectionTestUtils.setField(service, "maxRequestsPerMinute", 5);
        ReflectionTestUtils.setField(service, "windowMinutes", 1);
        ReflectionTestUtils.setField(service, "dailyLimit", 100);

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

        MetricsService metricsService = mock(MetricsService.class);
        when(metricsService.getRateLimitCounter()).thenReturn(mock(Counter.class));

        RateLimitService service = new RateLimitService(metricsService);
        ReflectionTestUtils.setField(service, "maxRequestsPerMinute", 5);
        ReflectionTestUtils.setField(service, "windowMinutes", 1);
        ReflectionTestUtils.setField(service, "dailyLimit", 100);

        service.checkLimit(1L);

        RateLimitStatus status = service.getStatus(1L);

        assertThat(status.getMinuteUsage()).isEqualTo(1);
        assertThat(status.getMinuteResetInSeconds())
                .isBetween(1L, 60L);
        assertThat(status.getMinuteResetTimeFormatted())
                .matches("00H (00|01)M [0-5][0-9]s|00H 01M 00s");
    }
}
