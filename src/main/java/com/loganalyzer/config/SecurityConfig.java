package com.loganalyzer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http

                // =====================================================
                // DISABLE CSRF (JWT BASED AUTH)
                // =====================================================
                .csrf(AbstractHttpConfigurer::disable)

                // =====================================================
                // ENABLE CORS
                // =====================================================
                .cors(Customizer.withDefaults())

                // =====================================================
                // AUTHORIZATION RULES
                // =====================================================
                .authorizeHttpRequests(auth -> auth

                        // IMPORTANT FOR SSE + ASYNC
                        .dispatcherTypeMatchers(
                                DispatcherType.ASYNC,
                                DispatcherType.FORWARD,
                                DispatcherType.ERROR
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register",
                                "/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health"
                        ).permitAll()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()

                        // EVERYTHING ELSE SECURED
                        .anyRequest().authenticated()
                )

                // =====================================================
                // EXCEPTION HANDLING
                // IMPORTANT FOR SSE STREAMS
                // =====================================================
                .exceptionHandling(ex -> ex

                        // UNAUTHORIZED
                        .authenticationEntryPoint(
                                (request, response, authException) -> {

                                    if (!response.isCommitted()) {

                                        writeSecurityError(
                                                request,
                                                response,
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized",
                                                "Authentication is required"
                                        );
                                    }
                                }
                        )

                        // ACCESS DENIED
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {

                                    if (!response.isCommitted()) {

                                        writeSecurityError(
                                                request,
                                                response,
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Forbidden",
                                                "Access is denied"
                                        );
                                    }
                                }
                        )
                )

                // =====================================================
                // STATELESS SESSION
                // =====================================================
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // =====================================================
                // AUTH PROVIDER
                // =====================================================
                .authenticationProvider(authenticationProvider())

                // =====================================================
                // JWT FILTER
                // =====================================================
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    // =========================================================
    // AUTH PROVIDER
    // =========================================================
    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();

        provider.setUserDetailsService(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    // =========================================================
    // AUTH MANAGER
    // =========================================================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }

    private void writeSecurityError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String error,
            String details
    ) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("error", error);
        body.put("details", details);
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
