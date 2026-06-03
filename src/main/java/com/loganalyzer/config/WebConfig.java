package com.loganalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // =========================================
        // ALLOW FRONTEND
        // =========================================
        config.setAllowedOrigins(
                parseAllowedOrigins()
        );

        // =========================================
        // ALLOW HEADERS
        // =========================================
        config.setAllowedHeaders(
                List.of("*")
        );

        // =========================================
        // ALLOW METHODS
        // =========================================
        config.setAllowedMethods(
                List.of("*")
        );

        // =========================================
        // ALLOW COOKIES/AUTH
        // =========================================
        config.setAllowCredentials(true);

        // =========================================
        // EXPOSE HEADERS
        // =========================================
        config.setExposedHeaders(
                List.of("Authorization")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
