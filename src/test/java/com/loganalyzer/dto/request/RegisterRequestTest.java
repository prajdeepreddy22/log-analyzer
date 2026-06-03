package com.loganalyzer.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresDisplayName() {
        RegisterRequest request = RegisterRequest.builder()
                .displayName("")
                .username("raj2122")
                .email("raj@example.com")
                .password("secret123")
                .build();

        Set<ConstraintViolation<RegisterRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getMessage().equals("Display name is required"));
    }

    @Test
    void acceptsValidDisplayName() {
        RegisterRequest request = RegisterRequest.builder()
                .displayName("Rajdeep")
                .username("raj2122")
                .email("raj@example.com")
                .password("secret123")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}
