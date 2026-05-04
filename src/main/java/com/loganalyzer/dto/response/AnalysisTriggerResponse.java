package com.loganalyzer.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisTriggerResponse {

    private String status;
    private String message;
    private String uploadId;
    private boolean canForce;
}