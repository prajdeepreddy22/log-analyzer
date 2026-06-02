package com.loganalyzer.controller;

import com.loganalyzer.dto.request.ChatRequest;
import com.loganalyzer.dto.response.ChatResponse;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    private Long getUserId(HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userId;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            HttpServletRequest httpRequest
    ) {

        Long userId = getUserId(httpRequest);

        log.info(
                "Chat API request uploadId={} userId={}",
                request.getUploadId(),
                userId
        );

        ChatResponse response =
                chatService.askQuestion(request, userId);

        return ResponseEntity.ok(response);
    }
}