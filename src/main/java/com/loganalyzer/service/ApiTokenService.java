package com.loganalyzer.service;

import com.loganalyzer.dto.request.CreateApiTokenRequest;
import com.loganalyzer.dto.response.ApiTokenResponse;
import com.loganalyzer.entity.ApiToken;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.ApiTokenRepository;
import com.loganalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiTokenService {

    private static final String TOKEN_PREFIX = "logai_live_";
    private static final int TOKEN_RANDOM_BYTES = 32;

    private final ApiTokenRepository apiTokenRepository;
    private final UserRepository userRepository;
    private final ApiTokenHashService apiTokenHashService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiTokenResponse createToken(
            Long userId,
            CreateApiTokenRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String rawToken = generateToken();

        ApiToken saved = apiTokenRepository.save(
                ApiToken.builder()
                        .user(user)
                        .tokenHash(apiTokenHashService.hash(rawToken))
                        .scope(ApiToken.Scope.INGEST)
                        .name(normalizeName(request.getName()))
                        .revoked(false)
                        .build()
        );

        return toResponse(saved)
                .token(rawToken)
                .message("Token created. Copy it now because it will not be shown again.")
                .build();
    }

    @Transactional(readOnly = true)
    public List<ApiTokenResponse> getTokens(Long userId) {

        return apiTokenRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(token -> toResponse(token).build())
                .toList();
    }

    @Transactional
    public void revokeToken(Long userId, Long tokenId) {

        ApiToken token = apiTokenRepository.findByIdAndUserId(tokenId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        token.setRevoked(true);
        apiTokenRepository.save(token);
    }

    @Transactional
    public ApiToken authenticateIngestToken(String rawToken) {

        ApiToken token = apiTokenRepository
                .findByTokenHashAndRevokedFalse(apiTokenHashService.hash(rawToken))
                .filter(item -> item.getScope() == ApiToken.Scope.INGEST)
                .orElseThrow(() -> new com.loganalyzer.exception.UnauthorizedException(
                        "Invalid ingestion token"
                ));

        token.setLastUsedAt(LocalDateTime.now());
        return apiTokenRepository.save(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return TOKEN_PREFIX + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private ApiTokenResponse.ApiTokenResponseBuilder toResponse(ApiToken token) {
        return ApiTokenResponse.builder()
                .id(token.getId())
                .scope(token.getScope().name())
                .name(token.getName())
                .createdAt(token.getCreatedAt())
                .lastUsedAt(token.getLastUsedAt())
                .revoked(Boolean.TRUE.equals(token.getRevoked()));
    }
}
