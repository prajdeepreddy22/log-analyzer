package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfidenceScoreServiceTest {

    private final ConfidenceScoreService service =
            new ConfidenceScoreService(new RootCauseNormalizer());

    @Test
    void givesHighConfidenceForStackTrace() {

        BigDecimal score = service.calculate(
                logs("java.lang.NullPointerException\n\tat com.app.Service.run(Service.java:42)"),
                "NULL_REFERENCE_ERROR"
        );

        assertThat(score).isBetween(
                new BigDecimal("0.850"),
                new BigDecimal("0.980")
        );
    }

    @Test
    void givesMediumHighConfidenceForKnownException() {

        BigDecimal score = service.calculate(
                logs("java.sql.SQLException: connection refused"),
                "DATABASE_CONNECTIVITY_FAILURE"
        );

        assertThat(score).isBetween(
                new BigDecimal("0.750"),
                new BigDecimal("0.900")
        );
    }

    @Test
    void givesMediumConfidenceForHttpEvidence() {

        BigDecimal score = service.calculate(
                logs("POST /api/orders returned status=503"),
                "INTERNAL_SERVER_FAILURE"
        );

        assertThat(score).isBetween(
                new BigDecimal("0.600"),
                new BigDecimal("0.800")
        );
    }

    @Test
    void givesLowConfidenceForNoisyUserBehavior() {

        BigDecimal score = service.calculate(
                logs("user clicked checkout button"),
                "INTERNAL_SERVER_FAILURE"
        );

        assertThat(score).isBetween(
                new BigDecimal("0.400"),
                new BigDecimal("0.650")
        );
    }

    @Test
    void unknownErrorAlwaysHasLowConfidence() {

        BigDecimal score = service.calculate(
                logs("java.lang.Exception\n\tat com.app.Service.run(Service.java:42)"),
                "UNKNOWN_ERROR"
        );

        assertThat(score).isLessThan(new BigDecimal("0.600"));
    }

    @Test
    void clampsValuesToValidRange() {

        assertThat(service.clamp(new BigDecimal("1.4")))
                .isEqualByComparingTo("1.000");
        assertThat(service.clamp(new BigDecimal("-0.2")))
                .isEqualByComparingTo("0.000");
        assertThat(service.clamp(null))
                .isEqualByComparingTo("0.000");
        assertThat(service.clamp(new BigDecimal("0.73")))
                .isEqualByComparingTo("0.730");
    }

    private List<Log> logs(String message) {
        return List.of(Log.builder().message(message).build());
    }
}
