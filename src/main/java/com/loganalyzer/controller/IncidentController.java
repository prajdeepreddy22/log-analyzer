package com.loganalyzer.controller;

import com.loganalyzer.dto.request.UpdateIncidentStatusRequest;
import com.loganalyzer.dto.response.IncidentResponse;
import com.loganalyzer.dto.response.IncidentStatusHistoryResponse;
import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.entity.Incident.IncidentStatus;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public PageResponse<IncidentResponse> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(
                        Sort.Order.desc("severityScore"),
                        Sort.Order.desc("occurrenceCount"),
                        Sort.Order.desc("lastSeen")
                )
        );

        log.info(
                "Fetching incidents userId={} status={} page={} size={}",
                userId,
                status,
                safePage,
                safeSize
        );

        return incidentService.getIncidents(userId, status, pageable);
    }

    @GetMapping("/{incidentId}")
    public IncidentResponse getIncident(
            @PathVariable String incidentId,
            HttpServletRequest request
    ) {

        return incidentService.getIncident(
                extractUserId(request),
                incidentId
        );
    }

    @PatchMapping("/{incidentId}/status")
    public IncidentResponse updateStatus(
            @PathVariable String incidentId,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            HttpServletRequest servletRequest
    ) {

        return incidentService.updateStatus(
                extractUserId(servletRequest),
                incidentId,
                request
        );
    }

    @GetMapping("/{incidentId}/history")
    public List<IncidentStatusHistoryResponse> getHistory(
            @PathVariable String incidentId,
            HttpServletRequest request
    ) {

        return incidentService.getHistory(
                extractUserId(request),
                incidentId
        );
    }

    private Long extractUserId(HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userId;
    }
}
