package com.loganalyzer.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void authenticatesStreamingChatRequestWithTokenQueryParam() throws Exception {

        SecurityContextHolder.clearContext();

        JwtService jwtService = mock(JwtService.class);
        UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        UserDetails userDetails = User.withUsername("raj2122")
                .password("password")
                .roles("USER")
                .build();

        when(jwtService.extractUsername("token-123")).thenReturn("raj2122");
        when(jwtService.extractUserId("token-123")).thenReturn(1L);
        when(userDetailsService.loadUserByUsername("raj2122")).thenReturn(userDetails);
        when(jwtService.isTokenValid("token-123", userDetails)).thenReturn(true);

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/chat/stream");
        request.setParameter("token", "token-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(request.getAttribute("userId")).isEqualTo(1L);
        assertThat(request.getAttribute("username")).isEqualTo("raj2122");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);

        SecurityContextHolder.clearContext();
    }
}
