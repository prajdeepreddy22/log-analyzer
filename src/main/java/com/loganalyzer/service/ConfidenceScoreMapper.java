package com.loganalyzer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ConfidenceScoreMapper {

    public static final int PRECISION = 4;
    public static final int SCALE = 3;
    public static final BigDecimal ZERO =
            BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    public static final BigDecimal ONE =
            BigDecimal.ONE.setScale(SCALE, RoundingMode.HALF_UP);

    private ConfidenceScoreMapper() {
    }

    public static BigDecimal toEntityValue(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }

        return value
                .max(ZERO)
                .min(ONE)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal toEntityValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return ZERO;
        }

        return toEntityValue(BigDecimal.valueOf(value));
    }

    public static Double toApiValue(BigDecimal value) {
        return toEntityValue(value).doubleValue();
    }
}
