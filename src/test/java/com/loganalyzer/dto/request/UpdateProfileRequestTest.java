package com.loganalyzer.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateProfileRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresDisplayName() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName("")
                .build();

        Set<ConstraintViolation<UpdateProfileRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .anyMatch(violation ->
                        violation.getMessage().equals("Display name is required"));
    }

    @Test
    void acceptsValidDisplayName() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .displayName("Rajdeep")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }
}
