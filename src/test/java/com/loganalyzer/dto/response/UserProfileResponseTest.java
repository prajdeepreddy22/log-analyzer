package com.loganalyzer.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileResponseTest {

    @Test
    void exposesProfileFields() {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(1L)
                .username("raj2122")
                .displayName("Rajdeep")
                .email("raj@example.com")
                .role("USER")
                .message("Profile updated successfully")
                .build();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDisplayName()).isEqualTo("Rajdeep");
        assertThat(response.getMessage())
                .isEqualTo("Profile updated successfully");
    }
}
