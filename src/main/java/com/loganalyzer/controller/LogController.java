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
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final LogQueryService logQueryService;

    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("logSequence", "logTimestamp", "level", "id");

    private Long extractUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userId;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String direction) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        String sortField = ALLOWED_SORT_FIELDS.contains(sortBy)
                ? sortBy
                : "logTimestamp";

        String dir = direction != null ? direction.toLowerCase() : "desc";

        Sort sort = dir.equals("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        return PageRequest.of(safePage, safeSize, sort);
    }

    @GetMapping("/{uploadId}")
    public PageResponse<LogResponse> getLogs(
            @PathVariable String uploadId,
            @RequestParam(required = false) com.loganalyzer.entity.Log.LogLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "logTimestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);
        Pageable pageable = buildPageable(page, size, sortBy, direction);

        log.info("Fetching logs uploadId={} userId={}", uploadId, userId);

        if (level != null) {
            LogFilterRequest filter = new LogFilterRequest();
            filter.setLevel(level);
            return logQueryService.searchLogs(uploadId, userId, filter, pageable);
        }

        return logQueryService.getLogs(uploadId, userId, pageable);
    }

    @GetMapping("/search")
    public PageResponse<LogResponse> searchLogsGet(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("q") String query,
            @RequestParam(required = false) com.loganalyzer.entity.Log.LogLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "logTimestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);
        Pageable pageable = buildPageable(page, size, sortBy, direction);

        LogFilterRequest filter = new LogFilterRequest();
        filter.setKeyword(query);
        filter.setLevel(level);

        log.info("Searching logs uploadId={} userId={} query={}", uploadId, userId, query);

        return logQueryService.searchLogs(uploadId, userId, filter, pageable);
    }

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

        Pageable pageable = buildPageable(
                filter.getPage(),
                filter.getSize(),
                filter.getSortBy() != null ? filter.getSortBy() : "logTimestamp",
                filter.getDirection() != null ? filter.getDirection() : "desc"
        );

        log.info("Searching logs uploadId={} userId={}", uploadId, userId);

        return logQueryService.searchLogs(uploadId, userId, filter, pageable);
    }

    @GetMapping("/{uploadId}/stats")
    public LogStatsResponse getStats(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);

        log.info("Fetching log stats uploadId={} userId={}", uploadId, userId);

        return logQueryService.getLogStats(uploadId, userId);
    }
}
