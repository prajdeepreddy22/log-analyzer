package com.loganalyzer.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

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

    @Test
    void returnsBadRequestForMalformedJson() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMalformedRequest(
                        new HttpMessageNotReadableException("internal parser detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("details", "The request body is missing or malformed");
    }

    @Test
    void returnsCorrectStatusForUnsupportedMediaAndMethod() {
        ResponseEntity<Map<String, Object>> mediaResponse =
                handler.handleUnsupportedMediaType(
                        new HttpMediaTypeNotSupportedException("text/plain"));
        ResponseEntity<Map<String, Object>> methodResponse =
                handler.handleUnsupportedMethod(
                        new HttpRequestMethodNotSupportedException("PUT"));

        assertThat(mediaResponse.getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(methodResponse.getStatusCode())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void genericErrorDoesNotExposeExceptionMessage() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGeneric(
                        new RuntimeException("jdbc:mysql://secret-host/password"));

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().get("details").toString())
                .doesNotContain("secret-host")
                .contains("Reference:");
    }

    @Test
    void returnsConflictForDuplicateResource() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleConflict(
                        new ConflictException("Username already exists"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody())
                .containsEntry("details", "Username already exists");
    }
}
