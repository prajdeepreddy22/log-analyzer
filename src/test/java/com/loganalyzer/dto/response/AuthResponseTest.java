package com.loganalyzer.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthResponseTest {

    @Test
    void includesDisplayName() {
        AuthResponse response = AuthResponse.builder()
                .token("token")
                .username("raj2122")
                .displayName("Rajdeep")
                .email("raj@example.com")
                .role("USER")
                .expiresIn(86_400_000)
                .message("Login successful")
                .build();

        assertThat(response.getDisplayName()).isEqualTo("Rajdeep");
        assertThat(response.getMessage()).isEqualTo("Login successful");
    }
}
