package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swaggerdocs.config.StorageConfig;
import com.swaggerdocs.model.SwaggerEntry;
import com.swaggerdocs.model.SwaggerMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class GitStorageService {

    private final String storagePath;
    private final ObjectMapper objectMapper;

    private Git git;
    private Path storageDir;

    public GitStorageService(StorageConfig config, ObjectMapper objectMapper) {
        this.storagePath = config.getPath();
        this.objectMapper = objectMapper;
    }

    public GitStorageService(String storagePath, ObjectMapper objectMapper) {
        this.storagePath = storagePath;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            storageDir = Path.of(storagePath);
            Files.createDirectories(storageDir);

            if (Files.exists(storageDir.resolve(".git"))) {
                git = Git.open(storageDir.toFile());
                log.info("Opened existing Git repository at {}", storageDir);
            } else {
                git = Git.init().setDirectory(storageDir.toFile()).call();
                log.info("Initialized new Git repository at {}", storageDir);
            }
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to initialize Git storage", e);
        }
    }

    public String save(String appName, JsonNode swagger, SwaggerMetadata metadata) {
        try {
            Path appDir = storageDir.resolve(appName);
            Files.createDirectories(appDir);

            Path swaggerFile = appDir.resolve("swagger.json");
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(swaggerFile.toFile(), swagger);

            Path metadataFile = appDir.resolve("metadata.json");
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(metadataFile.toFile(), metadata);

            git.add().addFilepattern(appName).call();

            String commitMessage = String.format("Update %s - %s",
                    appName,
                    metadata.getCommitHash() != null ? metadata.getCommitHash() : "manual");

            RevCommit commit = git.commit().setMessage(commitMessage).call();

            log.info("Saved swagger for {} at version {}", appName, commit.getId().abbreviate(7).name());
            return commit.getId().abbreviate(7).name();

        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to save swagger for " + appName, e);
        }
    }

    public Optional<JsonNode> getSwagger(String appName) {
        try {
            Path swaggerFile = storageDir.resolve(appName).resolve("swagger.json");
            if (Files.exists(swaggerFile)) {
                return Optional.of(objectMapper.readTree(swaggerFile.toFile()));
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read swagger for " + appName, e);
        }
    }

    public Optional<SwaggerMetadata> getMetadata(String appName) {
        try {
            Path metadataFile = storageDir.resolve(appName).resolve("metadata.json");
            if (Files.exists(metadataFile)) {
                return Optional.of(objectMapper.readValue(metadataFile.toFile(), SwaggerMetadata.class));
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read metadata for " + appName, e);
        }
    }

    public List<SwaggerEntry> listApps() {
        List<SwaggerEntry> entries = new ArrayList<>();
        try {
            if (!Files.exists(storageDir)) {
                return entries;
            }
            Files.list(storageDir)
                    .filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .forEach(appDir -> {
                        String appName = appDir.getFileName().toString();
                        getMetadata(appName).ifPresent(meta -> {
                            entries.add(SwaggerEntry.builder()
                                    .appName(meta.getAppName())
                                    .team(meta.getTeam())
                                    .version(meta.getVersion())
                                    .updatedAt(meta.getUpdatedAt())
                                    .build());
                        });
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list apps", e);
        }
        return entries;
    }

    public Optional<JsonNode> getSwaggerAtVersion(String appName, String commitHash) {
        try {
            var commitId = git.getRepository().resolve(commitHash);
            if (commitId == null) {
                return Optional.empty();
            }

            var treeWalk = org.eclipse.jgit.treewalk.TreeWalk.forPath(
                    git.getRepository(),
                    appName + "/swagger.json",
                    git.getRepository().parseCommit(commitId).getTree()
            );

            if (treeWalk == null) {
                return Optional.empty();
            }

            var loader = git.getRepository().open(treeWalk.getObjectId(0));
            return Optional.of(objectMapper.readTree(loader.getBytes()));

        } catch (IOException e) {
            throw new RuntimeException("Failed to get swagger at version " + commitHash, e);
        }
    }

    public List<String> getVersionHistory(String appName) {
        List<String> versions = new ArrayList<>();
        try {
            var logs = git.log().addPath(appName + "/swagger.json").call();
            for (RevCommit commit : logs) {
                versions.add(commit.getId().abbreviate(7).name());
            }
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to get version history for " + appName, e);
        }
        return versions;
    }
}
