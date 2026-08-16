package com.loganalyzer.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class SensitiveDataRedactionService {

    private static final String REDACTED = "[REDACTED]";
    private static final int MAX_REDACTION_INPUT_CHARACTERS = 20_000;
    private static final String REDACTION_TRUNCATION_MARKER =
            "\n[Content truncated before sensitive-data redaction]";

    private static final List<RedactionRule> RULES = List.of(
            new RedactionRule(
                    Pattern.compile(
                            "(?i)(authorization\\s*[:=]\\s*bearer\\s+)[A-Za-z0-9._~+/=-]+"
                    ),
                    "$1" + REDACTED
            ),
            new RedactionRule(
                    Pattern.compile(
                            "(?i)\\b(password|passwd|pwd|secret|client[_-]?secret|api[_-]?key|x[_-]?api[_-]?key|access[_-]?token|refresh[_-]?token|id[_-]?token|jwt|session[_-]?id|session|cookie|set-cookie|private[_-]?key)\\b\\s*[:=]\\s*([^\\s,;]+)"
                    ),
                    "$1=" + REDACTED
            ),
            new RedactionRule(
                    Pattern.compile(
                            "(?i)\\b(basic\\s+)[A-Za-z0-9+/=]{12,}"
                    ),
                    "$1" + REDACTED
            ),
            new RedactionRule(
                    Pattern.compile("\\bsk-[A-Za-z0-9_-]{12,}\\b"),
                    "sk-" + REDACTED
            ),
            new RedactionRule(
                    Pattern.compile(
                            "\\beyJ[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\.[A-Za-z0-9_-]{10,}\\b"
                    ),
                    "JWT_" + REDACTED
            ),
            new RedactionRule(
                    Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
                    "AWS_" + REDACTED
            ),
            new RedactionRule(
                    Pattern.compile("\\b\\d{13,19}\\b"),
                    "[REDACTED_CARD]"
            ),
            new RedactionRule(
                    Pattern.compile(
                            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
                    ),
                    "[REDACTED_EMAIL]"
            ),
            new RedactionRule(
                    Pattern.compile("\\b\\+?\\d{10,15}\\b"),
                    "[REDACTED_PHONE]"
            ),
            new RedactionRule(
                    Pattern.compile(
                            "(?i)(jdbc:[a-z0-9]+://)([^\\s/@:]+):([^\\s/@]+)@"
                    ),
                    "$1" + REDACTED + ":" + REDACTED + "@"
            ),
            new RedactionRule(
                    Pattern.compile(
                            "-----BEGIN ([A-Z ]+)?PRIVATE KEY-----[\\s\\S]*?-----END ([A-Z ]+)?PRIVATE KEY-----"
                    ),
                    "-----BEGIN PRIVATE KEY-----" + REDACTED + "-----END PRIVATE KEY-----"
            )
    );

    public String redact(String input) {

        if (input == null || input.isBlank()) {
            return input;
        }

        String redacted = limitInput(input);

        for (RedactionRule rule : RULES) {
            redacted = rule.pattern()
                    .matcher(redacted)
                    .replaceAll(rule.replacement());
        }

        return redacted;
    }

    private String limitInput(String input) {

        if (input.length() <= MAX_REDACTION_INPUT_CHARACTERS) {
            return input;
        }

        int retainedLength =
                MAX_REDACTION_INPUT_CHARACTERS
                        - REDACTION_TRUNCATION_MARKER.length();

        return input.substring(0, retainedLength)
                + REDACTION_TRUNCATION_MARKER;
    }

    private record RedactionRule(
            Pattern pattern,
            String replacement
    ) {
    }
}
