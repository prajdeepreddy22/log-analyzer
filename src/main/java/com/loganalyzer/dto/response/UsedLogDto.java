package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UsedLogDto {

    private Long id;

    private LocalDateTime timestamp;

    private String level;

    private String message;
}