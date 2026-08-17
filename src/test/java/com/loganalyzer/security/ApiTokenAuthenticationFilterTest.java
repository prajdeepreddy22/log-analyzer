package com.loganalyzer.security;

import com.loganalyzer.entity.ApiToken;
import com.loganalyzer.entity.User;
import com.loganalyzer.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiTokenAuthenticationFilterTest {

    @Test
    void authenticatesIngestionBearerTokenWithoutLoggingRawToken()
            throws Exception {

        SecurityContextHolder.clearContext();

        ApiTokenService apiTokenService = mock(ApiTokenService.class);
        ApiTokenAuthenticationFilter filter =
                new ApiTokenAuthenticationFilter(apiTokenService);

        User user = User.builder()
                .id(1L)
                .username("raj")
                .build();

        when(apiTokenService.authenticateIngestToken("raw-token"))
                .thenReturn(ApiToken.builder()
                        .id(5L)
                        .user(user)
                        .scope(ApiToken.Scope.INGEST)
                        .build());

        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/ingest/stream");
        request.setContextPath("/api");
        request.addHeader("Authorization", "Bearer raw-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("apiTokenUserId")).isEqualTo(1L);
        assertThat(request.getAttribute("apiTokenId")).isEqualTo(5L);
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull();
        verify(chain).doFilter(request, response);

        SecurityContextHolder.clearContext();
    }
}
