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
public class ValidationResult {
    private String status;
    private String version;
    private QualityScore quality;
    private List<BreakingChange> breakingChanges;
    private String viewUrl;
}
