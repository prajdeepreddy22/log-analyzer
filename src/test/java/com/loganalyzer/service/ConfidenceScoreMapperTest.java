package com.loganalyzer.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ConfidenceScoreMapperTest {

    @Test
    void defaultsNullToScaledZero() {
        assertThat(ConfidenceScoreMapper.toEntityValue((BigDecimal) null))
                .isEqualByComparingTo("0.000");
    }

    @Test
    void clampsAndRoundsToDatabaseScale() {
        assertThat(ConfidenceScoreMapper.toEntityValue(
                new BigDecimal("1.2345")
        )).isEqualByComparingTo("1.000");

        assertThat(ConfidenceScoreMapper.toEntityValue(
                new BigDecimal("0.7346")
        )).isEqualByComparingTo("0.735");
    }

    @Test
    void convertsEntityValueWithoutChangingApiType() {
        Double apiValue = ConfidenceScoreMapper.toApiValue(
                new BigDecimal("0.875")
        );

        assertThat(apiValue).isEqualTo(0.875);
    }
}
