package com.swaggerdocs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityScore {
    private int score;
    private List<QualityIssue> issues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityIssue {
        private String category;
        private String message;
        private String path;
    }
}
