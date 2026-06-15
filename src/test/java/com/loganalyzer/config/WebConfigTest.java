package com.loganalyzer.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void configuresExplicitJwtCorsContract() {
        WebConfig webConfig = new WebConfig();
        ReflectionTestUtils.setField(
                webConfig,
                "allowedOrigins",
                "https://frontend.example.com"
        );

        CorsFilter filter = webConfig.corsFilter();
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) ReflectionTestUtils.getField(
                        filter,
                        "configSource"
                );

        CorsConfiguration configuration =
                source.getCorsConfigurations().get("/**");

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://frontend.example.com");
        assertThat(configuration.getAllowedHeaders())
                .containsExactlyInAnyOrder("Authorization", "Content-Type");
        assertThat(configuration.getAllowedMethods())
                .containsExactlyInAnyOrderElementsOf(
                        List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                );
        assertThat(configuration.getAllowCredentials()).isFalse();
    }
}
