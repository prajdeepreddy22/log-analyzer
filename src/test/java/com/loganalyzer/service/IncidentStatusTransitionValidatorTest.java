package com.loganalyzer.service;

import com.loganalyzer.entity.Incident.IncidentStatus;
import com.loganalyzer.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentStatusTransitionValidatorTest {

    private final IncidentStatusTransitionValidator validator =
            new IncidentStatusTransitionValidator();

    @Test
    void allowsConfiguredIncidentStatusTransitions() {

        assertThatCode(() ->
                validator.validate(
                        IncidentStatus.OPEN,
                        IncidentStatus.INVESTIGATING
                ))
                .doesNotThrowAnyException();

        assertThatCode(() ->
                validator.validate(
                        IncidentStatus.INVESTIGATING,
                        IncidentStatus.FIXED
                ))
                .doesNotThrowAnyException();

        assertThatCode(() ->
                validator.validate(
                        IncidentStatus.FIXED,
                        IncidentStatus.VERIFIED
                ))
                .doesNotThrowAnyException();

        assertThatCode(() ->
                validator.validate(
                        IncidentStatus.VERIFIED,
                        IncidentStatus.CLOSED
                ))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidIncidentStatusTransitions() {

        assertThatThrownBy(() ->
                validator.validate(
                        IncidentStatus.CLOSED,
                        IncidentStatus.OPEN
                ))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Incident cannot move from CLOSED to OPEN");
    }
}
