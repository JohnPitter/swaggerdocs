package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swaggerdocs.config.GitRemoteConfig;
import com.swaggerdocs.config.StorageConfig;
import com.swaggerdocs.exception.GitSyncException;
import com.swaggerdocs.model.SwaggerEntry;
import com.swaggerdocs.model.SwaggerMetadata;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final GitRemoteConfig remoteConfig;

    private Git git;
    private Path storageDir;
    private CredentialsProvider credentialsProvider;

    @Autowired
    public GitStorageService(StorageConfig config, ObjectMapper objectMapper, GitRemoteConfig remoteConfig) {
        this.storagePath = config.getPath();
        this.objectMapper = objectMapper;
        this.remoteConfig = remoteConfig;
    }

    public GitStorageService(String storagePath, ObjectMapper objectMapper) {
        this.storagePath = storagePath;
        this.objectMapper = objectMapper;
        this.remoteConfig = new GitRemoteConfig(); // disabled by default
    }

    public GitStorageService(String storagePath, ObjectMapper objectMapper, GitRemoteConfig remoteConfig) {
        this.storagePath = storagePath;
        this.objectMapper = objectMapper;
        this.remoteConfig = remoteConfig;
    }

    @PostConstruct
    public void init() {
        try {
            storageDir = Path.of(storagePath);

            if (remoteConfig != null && remoteConfig.isConfigured()) {
                initializeCredentials();
                initializeFromRemote();
            } else {
                initializeLocal();
            }
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to initialize Git storage", e);
        }
    }

    private void initializeCredentials() {
        if (remoteConfig.getToken() != null) {
            credentialsProvider = new UsernamePasswordCredentialsProvider(
                    "oauth2", remoteConfig.getToken());
        }
    }

    private void initializeFromRemote() throws IOException, GitAPIException {
        if (!Files.exists(storageDir) || !Files.exists(storageDir.resolve(".git"))) {
            cloneRemote();
        } else {
            openAndPull();
        }
    }

    private void cloneRemote() throws GitAPIException {
        log.info("Cloning remote repository from {} (branch: {})",
                remoteConfig.getUrl(), remoteConfig.getBranch());

        git = Git.cloneRepository()
                .setURI(remoteConfig.getUrl())
                .setBranch(remoteConfig.getBranch())
                .setDirectory(storageDir.toFile())
                .setCredentialsProvider(credentialsProvider)
                .call();

        log.info("Successfully cloned remote repository to {}", storageDir);
    }

    private void openAndPull() throws IOException, GitAPIException {
        git = Git.open(storageDir.toFile());
        log.info("Opened existing repository at {}, pulling latest changes", storageDir);

        git.pull()
                .setCredentialsProvider(credentialsProvider)
                .call();

        log.info("Successfully pulled latest changes");
    }

    private void initializeLocal() throws IOException, GitAPIException {
        Files.createDirectories(storageDir);

        if (Files.exists(storageDir.resolve(".git"))) {
            git = Git.open(storageDir.toFile());
            log.info("Opened existing Git repository at {}", storageDir);
        } else {
            git = Git.init().setDirectory(storageDir.toFile()).call();
            log.info("Initialized new Git repository at {}", storageDir);
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

            pushWithRetry();

            return commit.getId().abbreviate(7).name();

        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to save swagger for " + appName, e);
        }
    }

    private void pushWithRetry() {
        if (remoteConfig == null || !remoteConfig.isConfigured()) {
            return;
        }

        int maxAttempts = remoteConfig.getRetry().getMaxAttempts();
        long delayMs = remoteConfig.getRetry().getDelayMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                git.push()
                        .setCredentialsProvider(credentialsProvider)
                        .call();
                log.info("Successfully pushed to remote");
                return;
            } catch (GitAPIException e) {
                log.warn("Push attempt {}/{} failed: {}", attempt, maxAttempts, e.getMessage());

                if (attempt >= maxAttempts) {
                    throw new GitSyncException(
                            "Push failed after " + maxAttempts + " attempts", e);
                }

                try {
                    long backoffDelay = delayMs * (1L << (attempt - 1));
                    Thread.sleep(backoffDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new GitSyncException("Push interrupted", ie);
                }
            }
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
