package com.swaggerdocs.service;

import com.swaggerdocs.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SwaggerService {

    private final GitStorageService gitStorageService;
    private final ValidationService validationService;
    private final DiffService diffService;
    private final String baseUrl;

    public SwaggerService(
            GitStorageService gitStorageService,
            ValidationService validationService,
            DiffService diffService,
            @Value("${swaggerdocs.base-url:http://localhost:8080}") String baseUrl) {
        this.gitStorageService = gitStorageService;
        this.validationService = validationService;
        this.diffService = diffService;
        this.baseUrl = baseUrl;
    }

    public ValidationResult processSubmission(SwaggerSubmission submission) {
        String appName = submission.getAppName();
        log.info("Processing swagger submission for app: {}", appName);

        var previousSwagger = gitStorageService.getSwagger(appName).orElse(null);

        QualityScore quality = validationService.calculateQuality(submission.getSwagger());
        log.debug("Quality score for {}: {}", appName, quality.getScore());

        List<BreakingChange> breakingChanges = diffService.findBreakingChanges(
                previousSwagger,
                submission.getSwagger()
        );

        SwaggerMetadata metadata = SwaggerMetadata.builder()
                .appName(appName)
                .team(submission.getTeam())
                .environment(submission.getEnvironment())
                .commitHash(submission.getMetadata() != null ? submission.getMetadata().getCommitHash() : null)
                .branch(submission.getMetadata() != null ? submission.getMetadata().getBranch() : null)
                .pipelineUrl(submission.getMetadata() != null ? submission.getMetadata().getPipelineUrl() : null)
                .updatedAt(Instant.now())
                .qualityScore(quality.getScore())
                .build();

        String version = gitStorageService.save(appName, submission.getSwagger(), metadata);

        String status = breakingChanges.isEmpty() ? "ACCEPTED" : "ACCEPTED_WITH_WARNINGS";
        log.info("Swagger {} processed: status={}, version={}, breakingChanges={}",
                appName, status, version, breakingChanges.size());

        return ValidationResult.builder()
                .status(status)
                .version(version)
                .quality(quality)
                .breakingChanges(breakingChanges)
                .viewUrl(baseUrl + "/docs/" + appName)
                .build();
    }

    public List<SwaggerEntry> listApps() {
        return gitStorageService.listApps();
    }

    public Optional<SwaggerInfo> getApp(String appName) {
        return gitStorageService.getSwagger(appName).map(swagger -> {
            var metadata = gitStorageService.getMetadata(appName).orElse(null);
            var quality = validationService.calculateQuality(swagger);
            var versions = gitStorageService.getVersionHistory(appName);

            return SwaggerInfo.builder()
                    .swagger(swagger)
                    .metadata(metadata)
                    .quality(quality)
                    .versions(versions)
                    .build();
        });
    }

    public Optional<com.fasterxml.jackson.databind.JsonNode> getSwaggerAtVersion(String appName, String version) {
        return gitStorageService.getSwaggerAtVersion(appName, version);
    }

    public List<BreakingChange> compareVersions(String appName, String fromVersion, String toVersion) {
        var fromSwagger = gitStorageService.getSwaggerAtVersion(appName, fromVersion).orElse(null);
        var toSwagger = toVersion.equals("current")
            ? gitStorageService.getSwagger(appName).orElse(null)
            : gitStorageService.getSwaggerAtVersion(appName, toVersion).orElse(null);

        return diffService.findBreakingChanges(fromSwagger, toSwagger);
    }

    public List<String> getVersionHistory(String appName) {
        return gitStorageService.getVersionHistory(appName);
    }
}
