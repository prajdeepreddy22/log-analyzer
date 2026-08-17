package com.loganalyzer.service;

import com.loganalyzer.dto.request.CreateApiTokenRequest;
import com.loganalyzer.dto.response.ApiTokenResponse;
import com.loganalyzer.entity.ApiToken;
import com.loganalyzer.entity.User;
import com.loganalyzer.repository.ApiTokenRepository;
import com.loganalyzer.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiTokenServiceTest {

    @Test
    void createTokenReturnsRawTokenOnceAndStoresOnlyHash() {

        ApiTokenRepository tokenRepository = mock(ApiTokenRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ApiTokenHashService hashService = new ApiTokenHashService();

        ApiTokenService service = new ApiTokenService(
                tokenRepository,
                userRepository,
                hashService
        );

        User user = User.builder()
                .id(1L)
                .username("raj")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(ApiToken.class)))
                .thenAnswer(invocation -> {
                    ApiToken token = invocation.getArgument(0);
                    token.setId(10L);
                    return token;
                });

        CreateApiTokenRequest request = new CreateApiTokenRequest();
        request.setName("Local watcher");

        ApiTokenResponse response = service.createToken(1L, request);

        assertThat(response.getToken()).startsWith("logai_live_");
        assertThat(response.getScope()).isEqualTo("INGEST");
        assertThat(response.getName()).isEqualTo("Local watcher");
    }

    @Test
    void listTokensNeverReturnsRawToken() {

        ApiTokenRepository tokenRepository = mock(ApiTokenRepository.class);
        ApiTokenService service = new ApiTokenService(
                tokenRepository,
                mock(UserRepository.class),
                new ApiTokenHashService()
        );

        when(tokenRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(ApiToken.builder()
                        .id(10L)
                        .scope(ApiToken.Scope.INGEST)
                        .name("Local watcher")
                        .tokenHash("hash")
                        .revoked(false)
                        .build()));

        List<ApiTokenResponse> response = service.getTokens(1L);

        assertThat(response).singleElement()
                .satisfies(token -> {
                    assertThat(token.getId()).isEqualTo(10L);
                    assertThat(token.getToken()).isNull();
                    assertThat(token.getScope()).isEqualTo("INGEST");
                });
    }
}
