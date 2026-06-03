package com.loganalyzer.security;

import com.loganalyzer.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void generatesTokenWithRawSecret() {
        JwtService jwtService = new JwtService(
                properties("plain-jwt-secret-for-local-dev-32-chars-min"));
        UserDetails userDetails = userDetails();

        String token = jwtService.generateToken(userDetails, 7L);

        assertThat(jwtService.extractUsername(token)).isEqualTo("raj2122");
        assertThat(jwtService.extractUserId(token)).isEqualTo(7L);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void generatesTokenWithBase64Secret() {
        String base64Secret = Base64.getEncoder().encodeToString(
                "base64-jwt-secret-for-tests-32-chars-min"
                        .getBytes(StandardCharsets.UTF_8));
        JwtService jwtService = new JwtService(properties(base64Secret));
        UserDetails userDetails = userDetails();

        String token = jwtService.generateToken(userDetails, 8L);

        assertThat(jwtService.extractUsername(token)).isEqualTo("raj2122");
        assertThat(jwtService.extractUserId(token)).isEqualTo(8L);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void rejectsShortRawSecret() {
        JwtService jwtService = new JwtService(properties("short-secret"));

        assertThatThrownBy(jwtService::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be at least 32 characters");
    }

    private AppProperties properties(String secret) {
        AppProperties properties = new AppProperties();
        properties.getJwt().setSecret(secret);
        properties.getJwt().setExpiration(86_400_000);
        return properties;
    }

    private UserDetails userDetails() {
        return new User(
                "raj2122",
                "password",
                List.of());
    }
}
