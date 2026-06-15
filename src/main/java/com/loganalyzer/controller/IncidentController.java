package com.loganalyzer.controller;

import com.loganalyzer.dto.response.IncidentResponse;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.IncidentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    public List<IncidentResponse> getIncidents(
            HttpServletRequest request
    ) {

        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        log.info("Fetching incidents userId={}", userId);

        return incidentService.getIncidents(userId);
    }
}
