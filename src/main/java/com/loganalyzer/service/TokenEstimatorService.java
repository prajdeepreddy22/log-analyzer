package com.loganalyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TokenEstimatorService {

    /*
     * Approximation:
     * 1 token ≈ 4 characters
     */
    private static final int AVG_CHARS_PER_TOKEN = 4;

    @Value("${app.ai.token.warning-threshold:6000}")
    private int warningThreshold;

    public int estimateTokens(String text) {

        if (text == null || text.isBlank()) {
            return 0;
        }

        int estimated =
                text.length() / AVG_CHARS_PER_TOKEN;

        log.debug(
                "Estimated token count={}",
                estimated
        );

        return estimated;
    }

    public boolean exceedsThreshold(String text) {

        int tokens = estimateTokens(text);

        boolean exceeded =
                tokens > warningThreshold;

        if (exceeded) {

            log.warn(
                    "Prompt token threshold exceeded. tokens={}",
                    tokens
            );
        }

        return exceeded;
    }
}