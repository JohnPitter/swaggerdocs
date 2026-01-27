package com.swaggerdocs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwaggerEntry {
    private String appName;
    private String team;
    private String version;
    private int qualityScore;
    private boolean hasBreakingChanges;
    private Instant updatedAt;
}
