package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiTokenResponse {

    private Long id;
    private String token;
    private String scope;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private Boolean revoked;
    private String message;
}
