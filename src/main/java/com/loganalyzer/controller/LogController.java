package com.loganalyzer.controller;

import com.loganalyzer.dto.request.LogFilterRequest;
import com.loganalyzer.dto.response.LogResponse;
import com.loganalyzer.service.LogQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Logs", description = "Log Search APIs")
public class LogController {

    private final LogQueryService logQueryService;

    @GetMapping("/{uploadId}")
    public Page<LogResponse> getLogs(
            @PathVariable String uploadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        log.info("Get logs called for uploadId: {}", uploadId); // ✅ added

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);

        LogFilterRequest request = new LogFilterRequest();

        return logQueryService.searchLogs(uploadId, request, pageable);
    }

    @PostMapping("/search/{uploadId}")
    public Page<LogResponse> searchLogs(
            @PathVariable String uploadId,
            @RequestBody LogFilterRequest request
    ) {

        log.info("Search logs called for uploadId: {}", uploadId); // ✅ added

        int safeSize = Math.min(request.getSize(), 100);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                safeSize
        );

        return logQueryService.searchLogs(uploadId, request, pageable);
    }
}