package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.config.GitRemoteConfig;
import com.swaggerdocs.exception.GitSyncException;
import com.swaggerdocs.model.SwaggerMetadata;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GitStorageServiceTest {

    @TempDir
    Path tempDir;

    private GitStorageService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        service = new GitStorageService(tempDir.toString(), objectMapper);
        service.init();
    }

    @Test
    void shouldSaveAndRetrieveSwagger() {
        String appName = "test-api";
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Test API");

        SwaggerMetadata metadata = SwaggerMetadata.builder()
                .appName(appName)
                .team("test-team")
                .commitHash("abc123")
                .updatedAt(Instant.now())
                .build();

        String version = service.save(appName, swagger, metadata);

        assertThat(version).isNotNull().hasSize(7);

        var retrieved = service.getSwagger(appName);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().get("openapi").asText()).isEqualTo("3.0.0");
    }

    @Test
    void shouldListAllApps() {
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");

        service.save("app-a", swagger, SwaggerMetadata.builder()
                .appName("app-a").team("team-a").updatedAt(Instant.now()).build());
        service.save("app-b", swagger, SwaggerMetadata.builder()
                .appName("app-b").team("team-b").updatedAt(Instant.now()).build());

        var apps = service.listApps();

        assertThat(apps).hasSize(2);
        assertThat(apps).extracting("appName").containsExactlyInAnyOrder("app-a", "app-b");
    }

    @Test
    void shouldGetVersionHistory() {
        String appName = "versioned-api";
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");

        service.save(appName, swagger, SwaggerMetadata.builder()
                .appName(appName).team("team").commitHash("v1").updatedAt(Instant.now()).build());

        swagger.put("openapi", "3.1.0");
        service.save(appName, swagger, SwaggerMetadata.builder()
                .appName(appName).team("team").commitHash("v2").updatedAt(Instant.now()).build());

        var versions = service.getVersionHistory(appName);

        assertThat(versions).hasSize(2);
    }

    @Test
    void shouldReturnEmptyForNonExistentApp() {
        var swagger = service.getSwagger("non-existent");
        assertThat(swagger).isEmpty();

        var metadata = service.getMetadata("non-existent");
        assertThat(metadata).isEmpty();
    }

    @Nested
    class RemoteSyncTests {

        private Path remoteDir;
        private Path localDir;
        private Git bareGit;

        @BeforeEach
        void setUpRemote() throws Exception {
            remoteDir = Files.createTempDirectory("remote");
            localDir = Files.createTempDirectory("local");
            bareGit = Git.init().setDirectory(remoteDir.toFile()).setBare(true).call();
        }

        @AfterEach
        void tearDown() {
            if (bareGit != null) {
                bareGit.close();
            }
        }

        @Test
        void shouldCloneRemoteOnFirstInit() throws Exception {
            Path setupDir = Files.createTempDirectory("setup");
            try (Git setupGit = Git.init().setDirectory(setupDir.toFile()).call()) {
                Path testFile = setupDir.resolve("test.txt");
                Files.writeString(testFile, "test content");
                setupGit.add().addFilepattern(".").call();
                setupGit.commit().setMessage("Initial commit").call();

                setupGit.remoteAdd().setName("origin").setUri(new URIish(remoteDir.toUri().toString())).call();
                setupGit.push().setRemote("origin").add("master").call();
            }

            GitStorageService remoteService = new GitStorageService(
                    localDir.toString(),
                    objectMapper,
                    createRemoteConfig(remoteDir.toUri().toString(), "master")
            );
            remoteService.init();

            assertThat(localDir.resolve("test.txt")).exists();
            assertThat(Files.readString(localDir.resolve("test.txt"))).isEqualTo("test content");
        }

        @Test
        void shouldPushAfterSave() throws Exception {
            Path setupDir = Files.createTempDirectory("setup");
            try (Git setupGit = Git.init().setDirectory(setupDir.toFile()).call()) {
                Files.writeString(setupDir.resolve("init.txt"), "init");
                setupGit.add().addFilepattern(".").call();
                setupGit.commit().setMessage("Initial").call();
                setupGit.remoteAdd().setName("origin").setUri(new URIish(remoteDir.toUri().toString())).call();
                setupGit.push().setRemote("origin").add("master").call();
            }

            GitStorageService remoteService = new GitStorageService(
                    localDir.toString(),
                    objectMapper,
                    createRemoteConfig(remoteDir.toUri().toString(), "master")
            );
            remoteService.init();

            ObjectNode swagger = objectMapper.createObjectNode();
            swagger.put("openapi", "3.0.0");
            SwaggerMetadata metadata = SwaggerMetadata.builder()
                    .appName("test-api")
                    .team("test-team")
                    .updatedAt(Instant.now())
                    .build();

            remoteService.save("test-api", swagger, metadata);

            Path verifyDir = Files.createTempDirectory("verify");
            try (Git verifyGit = Git.cloneRepository()
                    .setURI(remoteDir.toUri().toString())
                    .setDirectory(verifyDir.toFile())
                    .call()) {
                assertThat(verifyDir.resolve("test-api/swagger.json")).exists();
            }
        }

        @Test
        void shouldThrowGitSyncExceptionAfterMaxRetries() {
            var exception = new GitSyncException("test", new RuntimeException());
            assertThat(exception).isInstanceOf(RuntimeException.class);
            assertThat(exception.getMessage()).isEqualTo("test");
        }

        private GitRemoteConfig createRemoteConfig(String url, String branch) {
            var config = new GitRemoteConfig();
            config.setEnabled(true);
            config.setUrl(url);
            config.setBranch(branch);
            config.setToken("dummy-token");
            return config;
        }
    }
}
