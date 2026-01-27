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
public class SwaggerMetadata {
    private String appName;
    private String team;
    private String environment;
    private String version;
    private String commitHash;
    private String branch;
    private String pipelineUrl;
    private Instant updatedAt;
}
