package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatInsightDto {

    private String type;

    private String message;
}