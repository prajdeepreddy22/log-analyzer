package com.loganalyzer.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataRedactionServiceTest {

    private final SensitiveDataRedactionService service =
            new SensitiveDataRedactionService();

    @Test
    void redactsCommonSecretsAndPersonalData() {

        String input = """
                Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.secret.signature
                password=plainTextPassword
                api_key=sk-proj-1234567890abcdefghijklmnopqrstuvwxyz
                userEmail=demo.user@example.com
                awsKey=AKIAABCDEFGHIJKLMNOP
                jdbc:mysql://dbuser:dbpass@localhost:3306/log_analyzer
                """;

        String result = service.redact(input);

        assertThat(result)
                .contains("Authorization: Bearer [REDACTED]")
                .contains("password=[REDACTED]")
                .contains("api_key=[REDACTED]")
                .contains("userEmail=[REDACTED_EMAIL]")
                .contains("awsKey=AWS_[REDACTED]")
                .contains("jdbc:mysql://[REDACTED]:[REDACTED]@localhost:3306/log_analyzer")
                .doesNotContain("plainTextPassword")
                .doesNotContain("demo.user@example.com")
                .doesNotContain("dbpass")
                .doesNotContain("sk-proj-1234567890");
    }

    @Test
    void capsOversizedInputBeforeApplyingRedactionRules() {

        String result = service.redact(
                "ERROR " + "1".repeat(70_000)
        );

        assertThat(result)
                .hasSize(20_000)
                .endsWith("[Content truncated before sensitive-data redaction]");
    }
}
