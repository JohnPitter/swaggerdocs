package com.swaggerdocs.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SwaggerSubmission {
    @NotBlank(message = "appName is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "appName must contain only letters, numbers, hyphens and underscores")
    private String appName;

    @NotBlank(message = "team is required")
    private String team;

    private String environment;

    @NotNull(message = "swagger is required")
    private JsonNode swagger;

    private SubmissionMetadata metadata;

    @Data
    public static class SubmissionMetadata {
        private String commitHash;
        private String branch;
        private String pipelineUrl;
    }
}
