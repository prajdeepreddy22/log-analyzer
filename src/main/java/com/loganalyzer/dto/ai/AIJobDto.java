package com.loganalyzer.dto.ai;

import com.loganalyzer.entity.Log;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIJobDto {

    private String uploadId;

    private Long userId;

    private String hash;

    private List<Log> logs;
}