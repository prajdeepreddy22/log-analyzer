package com.loganalyzer.config;

import com.loganalyzer.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    // =========================================================
    // PUBLIC ENDPOINTS
    // =========================================================
    private static final String[] PUBLIC_URLS = {

            // AUTH
            "/auth/register",
            "/auth/login",

            // SWAGGER
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",

            // ACTUATOR
            "/actuator/health",
            "/actuator/info",

            // ERROR
            "/error"
    };

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

                        // PUBLIC URLS
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // SWAGGER WITH CONTEXT PATH
                        .requestMatchers(
                                "/api/v3/api-docs/**",
                                "/api/swagger-ui/**",
                                "/api/swagger-ui.html"
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

                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        );
                                    }
                                }
                        )

                        // ACCESS DENIED
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {

                                    if (!response.isCommitted()) {

                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Access Denied"
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
}
