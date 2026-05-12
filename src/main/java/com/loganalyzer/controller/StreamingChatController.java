package com.loganalyzer.controller;

import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.StreamingChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/chat")
public class StreamingChatController {

    private final StreamingChatService streamingChatService;

    // =====================================================
    // JWT USER EXTRACTION — matches your existing pattern
    // =====================================================
    private Long getUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userId;
    }

    // =====================================================
    // STREAMING CHAT — GET /chat/stream?message=...
    // =====================================================
    @GetMapping(
            value = "/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(required = false) String uploadId,
            HttpServletRequest request
    ) {
        Long userId = getUserId(request);

        log.info("Stream request userId={} uploadId={} message={}",
                userId, uploadId, message);

        return streamingChatService.streamResponse(message, uploadId, userId);
    }
}