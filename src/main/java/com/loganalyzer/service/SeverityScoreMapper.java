package com.loganalyzer.service;

public final class SeverityScoreMapper {

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    private SeverityScoreMapper() {
    }

    public static Byte toEntityValue(Integer value) {
        if (value == null) {
            return null;
        }

        return (byte) Math.max(MIN_SCORE, Math.min(MAX_SCORE, value));
    }

    public static Byte toEntityValue(Byte value) {
        return value == null
                ? null
                : toEntityValue(value.intValue());
    }

    public static Integer toApiValue(Byte value) {
        return value == null ? null : value.intValue();
    }

    public static int toAggregationValue(Byte value) {
        Integer apiValue = toApiValue(value);
        return apiValue == null ? MIN_SCORE : apiValue;
    }
}
