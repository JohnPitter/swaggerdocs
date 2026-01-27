package com.swaggerdocs.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class SwaggerSubmission {
    private String appName;
    private String team;
    private String environment;
    private JsonNode swagger;
    private SubmissionMetadata metadata;

    @Data
    public static class SubmissionMetadata {
        private String commitHash;
        private String branch;
        private String pipelineUrl;
    }
}
