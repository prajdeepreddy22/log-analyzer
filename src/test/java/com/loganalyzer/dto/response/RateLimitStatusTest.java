package com.loganalyzer.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitStatusTest {

    @Test
    void shouldExposeFrontendFriendlyUsageAliases() {

        RateLimitStatus status = RateLimitStatus.builder()
                .userId(1L)
                .minuteUsage(2)
                .minuteLimit(5)
                .dailyUsage(7)
                .dailyLimit(100)
                .resetInSeconds(30)
                .minuteResetInSeconds(30)
                .dailyResetInSeconds(3600)
                .blocked(false)
                .build();

        assertThat(status.getUsed()).isEqualTo(7);
        assertThat(status.getLimit()).isEqualTo(100);
        assertThat(status.getRemaining()).isEqualTo(93);
        assertThat(status.getMinuteRemaining()).isEqualTo(3);
        assertThat(status.getDailyRemaining()).isEqualTo(93);
        assertThat(status.getMinuteResetInSeconds()).isEqualTo(30);
        assertThat(status.getDailyResetInSeconds()).isEqualTo(3600);
    }
}
