package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {

    private String answer;

    private List<UsedLogDto> usedLogs;

    /*
     * DIRECT  -> fresh AI response
     * CACHED  -> reused/cached answer
     */
    private String source;

    private Integer confidence;

    // HIGH / MEDIUM / LOW
    private String quality;

    private List<String> insights;

    private List<RuleMatchResult> ruleMatches;

    private List<AnomalyResult> anomalies;
}