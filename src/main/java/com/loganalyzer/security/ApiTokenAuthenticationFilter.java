package com.loganalyzer.security;

import com.loganalyzer.entity.ApiToken;
import com.loganalyzer.service.ApiTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private final ApiTokenService apiTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!isIngestionRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawToken = extractBearerToken(request);

        if (rawToken == null || rawToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            ApiToken token = apiTokenService.authenticateIngestToken(rawToken);
            Long userId = token.getUser().getId();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "api-token-user-" + userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("apiTokenUserId", userId);
            request.setAttribute("apiTokenId", token.getId());
            request.setAttribute("apiTokenScope", token.getScope().name());

            log.debug("Authenticated ingestion token userId={}", userId);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute("authenticationError", "Invalid ingestion token");
            log.warn(
                    "Ingestion token rejected path={} reason={}",
                    request.getRequestURI(),
                    exception.getClass().getSimpleName()
            );
        }

        filterChain.doFilter(request, response);
    }

    private boolean isIngestionRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(request.getContextPath() + "/ingest/");
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7);
    }
}
