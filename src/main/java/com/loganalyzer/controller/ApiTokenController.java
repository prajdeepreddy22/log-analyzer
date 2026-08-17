package com.loganalyzer.controller;

import com.loganalyzer.dto.request.CreateApiTokenRequest;
import com.loganalyzer.dto.response.ApiTokenResponse;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.ApiTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/settings/tokens")
@RequiredArgsConstructor
@Slf4j
public class ApiTokenController {

    private final ApiTokenService apiTokenService;

    @PostMapping
    public ResponseEntity<ApiTokenResponse> createToken(
            @Valid @RequestBody CreateApiTokenRequest request,
            HttpServletRequest servletRequest
    ) {

        Long userId = extractUserId(servletRequest);

        log.info("Creating API token userId={}", userId);

        return ResponseEntity.ok(apiTokenService.createToken(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<ApiTokenResponse>> getTokens(
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);

        return ResponseEntity.ok(apiTokenService.getTokens(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> revokeToken(
            @PathVariable Long id,
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);

        apiTokenService.revokeToken(userId, id);

        return ResponseEntity.ok(
                Map.of("message", "Token revoked successfully")
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
