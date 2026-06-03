package com.loganalyzer.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsUnauthorizedForBadCredentials() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAuthentication(
                        new BadCredentialsException("Bad credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody())
                .containsEntry("status", HttpStatus.UNAUTHORIZED.value())
                .containsEntry("error", "Unauthorized")
                .containsEntry("details", "Invalid username or password");
    }
}
