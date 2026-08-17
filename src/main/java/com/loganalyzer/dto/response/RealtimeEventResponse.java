package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RealtimeEventResponse {

    private String type;

    private Object data;

    private LocalDateTime timestamp;

    public static RealtimeEventResponse of(
            String type,
            Object data
    ) {
        return RealtimeEventResponse.builder()
                .type(type)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
