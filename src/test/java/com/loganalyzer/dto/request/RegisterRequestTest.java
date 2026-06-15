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
                .password("Secret@123")
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
                .password("Secret@123")
                .build();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsInEmailAddress() {
        RegisterRequest request = validRequest();
        request.setEmail("raj@example.in");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsUnsupportedEmailDomain() {
        RegisterRequest request = validRequest();
        request.setEmail("raj@example.org");

        assertThat(validator.validate(request))
                .anyMatch(violation ->
                        violation.getMessage().equals(
                                "Email must be a valid .com or .in address"
                        ));
    }

    @Test
    void rejectsPasswordWithoutUppercaseLetter() {
        RegisterRequest request = validRequest();
        request.setPassword("secret@123");

        assertPasswordComplexityViolation(request);
    }

    @Test
    void rejectsPasswordWithoutLowercaseLetter() {
        RegisterRequest request = validRequest();
        request.setPassword("SECRET@123");

        assertPasswordComplexityViolation(request);
    }

    @Test
    void rejectsPasswordWithoutSpecialCharacter() {
        RegisterRequest request = validRequest();
        request.setPassword("Secret123");

        assertPasswordComplexityViolation(request);
    }

    @Test
    void rejectsPasswordShorterThanEightCharacters() {
        RegisterRequest request = validRequest();
        request.setPassword("Sec@123");

        assertThat(validator.validate(request))
                .anyMatch(violation ->
                        violation.getMessage().equals(
                                "Password must be between 8 and 255 characters"
                        ));
    }

    private RegisterRequest validRequest() {
        return RegisterRequest.builder()
                .displayName("Rajdeep")
                .username("raj2122")
                .email("raj@example.com")
                .password("Secret@123")
                .build();
    }

    private void assertPasswordComplexityViolation(RegisterRequest request) {
        assertThat(validator.validate(request))
                .anyMatch(violation ->
                        violation.getMessage().equals(
                                "Password must contain at least one uppercase letter, one lowercase letter, and one special character"
                        ));
    }
}
