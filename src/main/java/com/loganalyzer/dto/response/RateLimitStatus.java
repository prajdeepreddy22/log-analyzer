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

    public String getResetTimeFormatted() {
        return formatDuration(resetInSeconds);
    }

    public String getMinuteResetTimeFormatted() {
        return formatDuration(minuteResetInSeconds);
    }

    public String getDailyResetTimeFormatted() {
        return formatDuration(dailyResetInSeconds);
    }

    private String formatDuration(long seconds) {
        long safeSeconds = Math.max(0, seconds);
        long hours = safeSeconds / 3600;
        long minutes = (safeSeconds % 3600) / 60;
        long remainingSeconds = safeSeconds % 60;

        return String.format(
                "%02dH %02dM %02ds",
                hours,
                minutes,
                remainingSeconds
        );
    }
}
