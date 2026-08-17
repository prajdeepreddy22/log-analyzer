package com.loganalyzer.service;

import com.loganalyzer.entity.Incident.IncidentStatus;
import com.loganalyzer.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class IncidentStatusTransitionValidator {

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED =
            Map.of(
                    IncidentStatus.OPEN,
                    Set.of(IncidentStatus.INVESTIGATING, IncidentStatus.CLOSED),

                    IncidentStatus.INVESTIGATING,
                    Set.of(IncidentStatus.FIXED, IncidentStatus.CLOSED),

                    IncidentStatus.FIXED,
                    Set.of(IncidentStatus.VERIFIED, IncidentStatus.INVESTIGATING),

                    IncidentStatus.VERIFIED,
                    Set.of(IncidentStatus.CLOSED),

                    IncidentStatus.CLOSED,
                    Set.of()
            );

    public void validate(
            IncidentStatus from,
            IncidentStatus to
    ) {

        if (from == null || to == null) {
            throw new InvalidStatusTransitionException(
                    "Incident status transition is invalid"
            );
        }

        if (from == to) {
            return;
        }

        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new InvalidStatusTransitionException(
                    "Incident cannot move from " + from + " to " + to
            );
        }
    }
}
