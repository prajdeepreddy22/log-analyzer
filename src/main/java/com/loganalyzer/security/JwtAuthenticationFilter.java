package com.loganalyzer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        } else if (isStreamingChatRequest(request)) {
            jwt = request.getParameter("token");
        }

        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(jwt);
            Long userId = jwtService.extractUserId(jwt); // ✅ IMPORTANT

            if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);

                    // ✅ CRITICAL FOR USER ISOLATION
                    if (userId != null) {
                        request.setAttribute("userId", userId);
                    }

                    request.setAttribute("username", username);

                    log.debug("Authenticated user: {}", username);
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            request.setAttribute("authenticationError", "Invalid or expired token");
            log.warn(
                    "JWT authentication rejected path={} reason={}",
                    request.getRequestURI(),
                    e.getClass().getSimpleName()
            );
        }

        filterChain.doFilter(request, response);
    }

    private boolean isStreamingChatRequest(HttpServletRequest request) {

        String uri = request.getRequestURI();

        return "GET".equalsIgnoreCase(request.getMethod())
                && uri != null
                && uri.endsWith("/chat/stream");
    }
}
