package com.loganalyzer.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresUsernameAndPassword() {
        LoginRequest request = LoginRequest.builder()
                .username("")
                .password("")
                .build();

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getMessage())
                .contains("Username is required", "Password is required");
    }
}
