package com.loganalyzer.controller;

import com.loganalyzer.dto.request.CreateLogSourceRequest;
import com.loganalyzer.dto.request.UpdateLogSourceStatusRequest;
import com.loganalyzer.dto.response.LogSourceResponse;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.LogSourceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/log-sources")
@RequiredArgsConstructor
@Slf4j
public class LogSourceController {

    private final LogSourceService logSourceService;

    @PostMapping
    public ResponseEntity<LogSourceResponse> createSource(
            @Valid @RequestBody CreateLogSourceRequest request,
            HttpServletRequest servletRequest
    ) {

        Long userId = extractUserId(servletRequest);

        log.info("Creating log ingestion source userId={}", userId);

        return ResponseEntity.ok(logSourceService.createSource(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<LogSourceResponse>> getSources(
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);

        return ResponseEntity.ok(logSourceService.getSources(userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LogSourceResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLogSourceStatusRequest request,
            HttpServletRequest servletRequest
    ) {

        Long userId = extractUserId(servletRequest);

        return ResponseEntity.ok(
                logSourceService.updateStatus(userId, id, request)
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
