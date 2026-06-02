package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateLimitStatus {

    private Long userId;

    private int minuteUsage;

    private int minuteLimit;

    private int dailyUsage;

    private int dailyLimit;

    private long resetInSeconds;

    private long minuteResetInSeconds;

    private long dailyResetInSeconds;

    private boolean blocked;

    public int getUsed() {
        return dailyUsage;
    }

    public int getLimit() {
        return dailyLimit;
    }

    public int getRemaining() {
        return Math.max(0, dailyLimit - dailyUsage);
    }

    public int getMinuteRemaining() {
        return Math.max(0, minuteLimit - minuteUsage);
    }

    public int getDailyRemaining() {
        return Math.max(0, dailyLimit - dailyUsage);
    }
}
