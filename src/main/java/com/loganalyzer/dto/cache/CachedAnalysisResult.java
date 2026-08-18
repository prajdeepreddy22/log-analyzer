package com.loganalyzer.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedAnalysisResult {

    private String summary;

    private String rootCause;

    private String developerMistake;

    private String fixSuggestion;

    private String codeFix;

    private Byte severityScore;

    private BigDecimal confidenceScore;
}
