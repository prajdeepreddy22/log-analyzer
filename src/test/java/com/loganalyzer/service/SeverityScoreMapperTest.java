package com.loganalyzer.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeverityScoreMapperTest {

    @Test
    void clampsValuesToTinyIntDomainUsedByApplication() {
        assertThat(SeverityScoreMapper.toEntityValue(-1))
                .isEqualTo((byte) 1);
        assertThat(SeverityScoreMapper.toEntityValue(3))
                .isEqualTo((byte) 3);
        assertThat(SeverityScoreMapper.toEntityValue(9))
                .isEqualTo((byte) 5);
    }

    @Test
    void preservesNullAndConvertsForApi() {
        assertThat(SeverityScoreMapper.toEntityValue((Integer) null))
                .isNull();
        assertThat(SeverityScoreMapper.toApiValue((byte) 4))
                .isEqualTo(4);
    }
}
