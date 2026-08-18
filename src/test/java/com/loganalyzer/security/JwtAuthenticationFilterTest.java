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

        when(jwtService.extractUsername("token-123")).thenReturn("Raj2122");
        when(jwtService.extractUserId("token-123")).thenReturn(1L);
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);
        when(jwtService.isTokenValidForUserId("token-123", 1L)).thenReturn(true);

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

    @Test
    void authenticatesRealtimeEventStreamWithTokenQueryParam() throws Exception {

        SecurityContextHolder.clearContext();

        JwtService jwtService = mock(JwtService.class);
        UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        UserDetails userDetails = User.withUsername("raj2122")
                .password("password")
                .roles("USER")
                .build();

        when(jwtService.extractUsername("token-123")).thenReturn("Raj2122");
        when(jwtService.extractUserId("token-123")).thenReturn(1L);
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetails);
        when(jwtService.isTokenValidForUserId("token-123", 1L)).thenReturn(true);

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/events/stream");
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

    @Test
    void invalidTokenDoesNotAuthenticateOrExposeStackTraceToResponse()
            throws Exception {

        SecurityContextHolder.clearContext();

        JwtService jwtService = mock(JwtService.class);
        UserDetailsServiceImpl userDetailsService =
                mock(UserDetailsServiceImpl.class);
        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        when(jwtService.extractUsername("invalid"))
                .thenThrow(new IllegalArgumentException("secret detail"));

        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/uploads");
        request.addHeader("Authorization", "Bearer invalid");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNull();
        assertThat(request.getAttribute("authenticationError"))
                .isEqualTo("Invalid or expired token");
        assertThat(response.getContentAsString())
                .doesNotContain("secret detail");
        verify(filterChain).doFilter(request, response);
    }
}
