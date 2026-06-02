package com.loganalyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();

        // =========================================
        // ALLOW FRONTEND
        // =========================================
        config.setAllowedOrigins(
                List.of("http://localhost:4200")
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
}