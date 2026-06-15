package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ConfidenceScoreService {

    private final RootCauseNormalizer rootCauseNormalizer;

    public BigDecimal calculate(
            List<Log> logs,
            String rootCause
    ) {

        String normalizedRootCause =
                rootCauseNormalizer.normalize(rootCause);

        if (RootCauseNormalizer.RootCauseCategory.UNKNOWN_ERROR.name()
                .equals(normalizedRootCause)) {
            return ConfidenceScoreMapper.toEntityValue(0.55);
        }

        String combined = combineMessages(logs);

        if (hasStackTrace(combined)) {
            return ConfidenceScoreMapper.toEntityValue(0.95);
        }

        if (hasKnownException(combined)) {
            return ConfidenceScoreMapper.toEntityValue(0.85);
        }

        if (hasHttpEvidence(combined)) {
            return ConfidenceScoreMapper.toEntityValue(0.72);
        }

        if (hasNoisyUserBehavior(combined)) {
            return ConfidenceScoreMapper.toEntityValue(0.50);
        }

        return ConfidenceScoreMapper.toEntityValue(0.65);
    }

    public BigDecimal clamp(BigDecimal confidenceScore) {
        return ConfidenceScoreMapper.toEntityValue(confidenceScore);
    }

    private String combineMessages(List<Log> logs) {

        if (logs == null || logs.isEmpty()) {
            return "";
        }

        return logs.stream()
                .map(Log::getMessage)
                .filter(Objects::nonNull)
                .map(message -> message.toLowerCase(Locale.ROOT))
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private boolean hasStackTrace(String text) {
        return text.contains("\n\tat ")
                || text.contains("\nat ")
                || text.contains("caused by:")
                || text.contains("suppressed:");
    }

    private boolean hasKnownException(String text) {
        return containsAny(
                text,
                "nullpointerexception",
                "outofmemoryerror",
                "sqlexception",
                "sockettimeoutexception",
                "connectexception",
                "illegalstateexception",
                "illegalargumentexception",
                "exception",
                " error"
        );
    }

    private boolean hasHttpEvidence(String text) {
        return containsAny(
                text,
                "http/1.1",
                "http/2",
                "status=",
                "status code",
                "request method",
                "get /",
                "post /",
                "put /",
                "patch /",
                "delete /",
                " 400 ",
                " 401 ",
                " 403 ",
                " 404 ",
                " 429 ",
                " 500 ",
                " 502 ",
                " 503 ",
                " 504 "
        );
    }

    private boolean hasNoisyUserBehavior(String text) {
        return containsAny(
                text,
                "user clicked",
                "page viewed",
                "navigation",
                "session started",
                "session ended",
                "login attempt",
                "button click",
                "screen opened"
        );
    }

    private boolean containsAny(String text, String... keywords) {

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
