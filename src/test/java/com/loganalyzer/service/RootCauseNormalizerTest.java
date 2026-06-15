package com.loganalyzer.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RootCauseNormalizerTest {

    private final RootCauseNormalizer normalizer =
            new RootCauseNormalizer();

    @ParameterizedTest
    @CsvSource({
            "TIMEOUT, NETWORK_TIMEOUT",
            "gateway timeout, NETWORK_TIMEOUT",
            "DATABASE_ERROR, DATABASE_CONNECTIVITY_FAILURE",
            "java.sql.SQLException, DATABASE_CONNECTIVITY_FAILURE",
            "MEMORY_ISSUE, MEMORY_EXHAUSTION",
            "java.lang.OutOfMemoryError: Java heap space, MEMORY_EXHAUSTION",
            "NULL_POINTER_EXCEPTION, NULL_REFERENCE_ERROR",
            "java.lang.NullPointerException, NULL_REFERENCE_ERROR",
            "APPLICATION_ERROR, INTERNAL_SERVER_FAILURE",
            "INTERNAL_SERVER_ERROR, INTERNAL_SERVER_FAILURE",
            "NETWORK_TIMEOUT, NETWORK_TIMEOUT",
            "DATABASE_CONNECTIVITY_FAILURE, DATABASE_CONNECTIVITY_FAILURE",
            "MEMORY_EXHAUSTION, MEMORY_EXHAUSTION",
            "NULL_REFERENCE_ERROR, NULL_REFERENCE_ERROR",
            "INTERNAL_SERVER_FAILURE, INTERNAL_SERVER_FAILURE",
            "UNKNOWN_ERROR, UNKNOWN_ERROR"
    })
    void normalizesKnownRootCauses(
            String rawRootCause,
            String expectedCategory
    ) {

        assertThat(normalizer.normalize(rawRootCause))
                .isEqualTo(expectedCategory);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "UNKNOWN", "N/A", "unexpected value"})
    void mapsMissingOrUnrecognizedValuesToUnknown(String rawRootCause) {

        assertThat(normalizer.normalize(rawRootCause))
                .isEqualTo("UNKNOWN_ERROR");
    }
}
