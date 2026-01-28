package com.swaggerdocs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.config.GitRemoteConfig;
import com.swaggerdocs.model.SwaggerMetadata;
import com.swaggerdocs.service.GitStorageService;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GitRemoteSyncIntegrationTest {

    private Path remoteDir;
    private Path instance1Dir;
    private Path instance2Dir;
    private ObjectMapper objectMapper;
    private Git bareGit;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        remoteDir = Files.createTempDirectory("remote");
        instance1Dir = Files.createTempDirectory("instance1");
        instance2Dir = Files.createTempDirectory("instance2");
    }

    @AfterEach
    void tearDown() {
        if (bareGit != null) {
            bareGit.close();
        }
    }

    @Test
    void shouldSyncBetweenInstances() throws Exception {
        // Setup bare remote (simulates GitHub)
        bareGit = Git.init().setDirectory(remoteDir.toFile()).setBare(true).call();

        // Create initial commit in remote
        Path setupDir = Files.createTempDirectory("setup");
        try (Git setupGit = Git.init().setDirectory(setupDir.toFile()).call()) {
            Files.writeString(setupDir.resolve(".gitkeep"), "");
            setupGit.add().addFilepattern(".").call();
            setupGit.commit().setMessage("Initial").call();
            setupGit.remoteAdd().setName("origin").setUri(new URIish(remoteDir.toUri().toString())).call();
            setupGit.push().setRemote("origin").add("master").call();
        }

        // Instance 1: Clone and save
        GitRemoteConfig config1 = createConfig(remoteDir.toUri().toString());
        GitStorageService service1 = new GitStorageService(instance1Dir.toString(), objectMapper, config1);
        service1.init();

        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Payments API");

        service1.save("payments-api", swagger, SwaggerMetadata.builder()
                .appName("payments-api")
                .team("payments")
                .version("1.0.0")
                .updatedAt(Instant.now())
                .build());

        // Instance 2: Clone (simulates new pod) - should see payments-api
        GitRemoteConfig config2 = createConfig(remoteDir.toUri().toString());
        GitStorageService service2 = new GitStorageService(instance2Dir.toString(), objectMapper, config2);
        service2.init();

        var apps = service2.listApps();
        assertThat(apps).hasSize(1);
        assertThat(apps.get(0).getAppName()).isEqualTo("payments-api");

        var retrieved = service2.getSwagger("payments-api");
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().get("info").get("title").asText()).isEqualTo("Payments API");
    }

    @Test
    void shouldPullUpdatesOnRestart() throws Exception {
        // Setup bare remote
        bareGit = Git.init().setDirectory(remoteDir.toFile()).setBare(true).call();

        // Create initial commit
        Path setupDir = Files.createTempDirectory("setup");
        try (Git setupGit = Git.init().setDirectory(setupDir.toFile()).call()) {
            Files.writeString(setupDir.resolve(".gitkeep"), "");
            setupGit.add().addFilepattern(".").call();
            setupGit.commit().setMessage("Initial").call();
            setupGit.remoteAdd().setName("origin").setUri(new URIish(remoteDir.toUri().toString())).call();
            setupGit.push().setRemote("origin").add("master").call();
        }

        // Instance 1: Save first API
        GitRemoteConfig config = createConfig(remoteDir.toUri().toString());
        GitStorageService service1 = new GitStorageService(instance1Dir.toString(), objectMapper, config);
        service1.init();

        ObjectNode swagger1 = objectMapper.createObjectNode();
        swagger1.put("openapi", "3.0.0");
        service1.save("api-v1", swagger1, SwaggerMetadata.builder()
                .appName("api-v1")
                .team("team-a")
                .updatedAt(Instant.now())
                .build());

        // Instance 2: Clone and verify api-v1 exists
        GitStorageService service2 = new GitStorageService(instance2Dir.toString(), objectMapper, config);
        service2.init();
        assertThat(service2.listApps()).hasSize(1);

        // Instance 1: Save second API
        ObjectNode swagger2 = objectMapper.createObjectNode();
        swagger2.put("openapi", "3.1.0");
        service1.save("api-v2", swagger2, SwaggerMetadata.builder()
                .appName("api-v2")
                .team("team-b")
                .updatedAt(Instant.now())
                .build());

        // Instance 2: Reinitialize (simulates pod restart) - should pull and see both APIs
        GitStorageService service2Restarted = new GitStorageService(instance2Dir.toString(), objectMapper, config);
        service2Restarted.init();

        var apps = service2Restarted.listApps();
        assertThat(apps).hasSize(2);
        assertThat(apps).extracting("appName").containsExactlyInAnyOrder("api-v1", "api-v2");
    }

    private GitRemoteConfig createConfig(String url) {
        var config = new GitRemoteConfig();
        config.setEnabled(true);
        config.setUrl(url);
        config.setBranch("master");
        config.setToken("dummy");
        return config;
    }
}
