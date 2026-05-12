package com.loganalyzer.controller;

import com.loganalyzer.dto.response.RateLimitStatus;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rate-limit")
@RequiredArgsConstructor
@Slf4j
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @GetMapping("/status")
    public RateLimitStatus getStatus(
            HttpServletRequest request) {

        Long userId =
                (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException(
                    "Unauthorized"
            );
        }

        log.info(
                "Fetching rate limit status userId={}",
                userId
        );

        return rateLimitService.getStatus(userId);
    }
}