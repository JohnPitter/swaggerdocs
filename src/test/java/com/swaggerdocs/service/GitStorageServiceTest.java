package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.model.SwaggerMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
