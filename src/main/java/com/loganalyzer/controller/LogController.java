package com.loganalyzer.controller;

import com.loganalyzer.dto.request.LogFilterRequest;
import com.loganalyzer.dto.response.LogResponse;
import com.loganalyzer.dto.response.LogStatsResponse;
import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.exception.BadRequestException;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.LogQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final LogQueryService logQueryService;

    private Long extractUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userId;
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize);
    }

    // GET logs
    @GetMapping("/{uploadId}")
    public PageResponse<LogResponse> getLogs(
            @PathVariable String uploadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        Pageable pageable = buildPageable(page, size);

        return logQueryService.getLogs(uploadId, userId, pageable);
    }

    // FILTER search
    @PostMapping("/search/{uploadId}")
    public PageResponse<LogResponse> searchLogs(
            @PathVariable String uploadId,
            @RequestBody LogFilterRequest filter,
            HttpServletRequest request
    ) {
        if (filter == null) {
            throw new BadRequestException("Invalid request body");
        }

        Long userId = extractUserId(request);
        Pageable pageable = buildPageable(filter.getPage(), filter.getSize());

        return logQueryService.searchLogs(uploadId, userId, filter, pageable);
    }

    // STATS
    @GetMapping("/{uploadId}/stats")
    public LogStatsResponse getStats(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {
        Long userId = extractUserId(request);
        return logQueryService.getLogStats(uploadId, userId);
    }
}