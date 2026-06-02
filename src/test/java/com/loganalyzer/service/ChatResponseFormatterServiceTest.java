package com.loganalyzer.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatResponseFormatterServiceTest {

    private final ChatResponseFormatterService formatter =
            new ChatResponseFormatterService();

    @Test
    void shouldImproveCrampedChatResponseFormatting() {

        String cramped = """
                The logs indicate two primary issues:

                1.NullPointerException in AuthService:

                Timestamp: 2026-04-17 10:15:32
                This error suggests that the AuthService encountered a null reference while processing a login request.The root cause detection confirms this as a NULL_POINTER_EXCEPTION with a high confidence score (95%).Fix Suggestions:
                Review the login processing code in AuthService to identify potential null references, especially in user input handling or database retrieval.- Implement null checks and exception handling to prevent the application from crashing.- Consider adding logging around the login process to capture more context when the error occurs.2.Payment gateway timeout in PaymentService:
                """;

        String formatted = formatter.format(cramped);

        assertThat(formatted).contains("1. NullPointerException in AuthService:");
        assertThat(formatted).contains("login request. The root cause detection");
        assertThat(formatted).contains("confidence score (95%).\n\nFix Suggestions:");
        assertThat(formatted).contains("database retrieval.\n- Implement null checks");
        assertThat(formatted).contains("application from crashing.\n- Consider adding logging");
        assertThat(formatted).contains("error occurs.\n\n2. Payment gateway timeout in PaymentService:");
    }

    @Test
    void shouldFormatIssueEvidenceAndFixSections() {

        String cramped = """
                Issues
                1.Null Pointer Exception in AuthService

                Evidence:
                [ERROR] AuthService : 2026-04-17 10:15:32 ERROR [AuthService] NullPointerException occurred while processing login
                Fix:
                Investigate the login processing code for potential null references.2.Payment Gateway Timeout
                Evidence:
                [ERROR] PaymentService : 2026-04-17 10:15:42 ERROR [PaymentService] Payment gateway timeout
                Fix:
                Check the payment gateway connectivity and response time.
                """;

        String formatted = formatter.format(cramped);

        assertThat(formatted).contains("1. Null Pointer Exception in AuthService");
        assertThat(formatted).contains("\n\nEvidence:\n[ERROR] AuthService");
        assertThat(formatted).contains("\n\nFix:\nInvestigate the login processing code");
        assertThat(formatted).contains("potential null references.\n\n2. Payment Gateway Timeout");
        assertThat(formatted).contains("\n\nEvidence:\n[ERROR] PaymentService");
    }
}
