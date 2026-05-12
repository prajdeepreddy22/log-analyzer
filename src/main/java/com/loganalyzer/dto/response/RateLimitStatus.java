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

    private boolean blocked;
}