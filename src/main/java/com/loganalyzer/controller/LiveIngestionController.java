package com.loganalyzer.controller;

import com.loganalyzer.dto.request.LiveIngestionRequest;
import com.loganalyzer.dto.response.LiveIngestionResponse;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.LogStreamIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ingest")
@RequiredArgsConstructor
@Slf4j
public class LiveIngestionController {

    private final LogStreamIngestionService logStreamIngestionService;

    @PostMapping("/stream")
    public ResponseEntity<LiveIngestionResponse> ingest(
            @Valid @RequestBody LiveIngestionRequest request,
            HttpServletRequest servletRequest
    ) {

        Long userId = (Long) servletRequest.getAttribute("apiTokenUserId");

        if (userId == null) {
            throw new UnauthorizedException("Invalid ingestion token");
        }

        LiveIngestionResponse response =
                logStreamIngestionService.ingest(userId, request);

        return ResponseEntity
                .status(response.isDuplicate() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(response);
    }
}
