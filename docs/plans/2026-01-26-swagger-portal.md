# SwaggerDocs Portal - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Portal centralizado para documentação de APIs da empresa, recebendo Swaggers via CI e oferecendo validação de qualidade + detecção de breaking changes.

**Architecture:** Spring Boot com Git local (JGit) para versionamento, Thymeleaf para páginas server-rendered, Swagger UI e Redoc embebidos via WebJars para visualização.

**Tech Stack:** Java 21, Spring Boot 3.x, JGit, openapi-diff, Thymeleaf, WebJars (Swagger UI, Redoc)

---

## Task 1: Project Setup

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/swaggerdocs/SwaggerDocsApplication.java`
- Create: `src/main/resources/application.yml`

**Step 1: Initialize Maven project with dependencies**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
    </parent>

    <groupId>com.swaggerdocs</groupId>
    <artifactId>swaggerdocs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>SwaggerDocs Portal</name>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Thymeleaf -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>

        <!-- JGit -->
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
            <version>6.8.0.202311291450-r</version>
        </dependency>

        <!-- OpenAPI Diff -->
        <dependency>
            <groupId>org.openapitools.openapidiff</groupId>
            <artifactId>openapi-diff-core</artifactId>
            <version>2.1.0-beta.8</version>
        </dependency>

        <!-- Swagger UI WebJar -->
        <dependency>
            <groupId>org.webjars</groupId>
            <artifactId>swagger-ui</artifactId>
            <version>5.11.0</version>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

**Step 2: Create main application class**

```java
package com.swaggerdocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SwaggerDocsApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwaggerDocsApplication.class, args);
    }
}
```

**Step 3: Create application.yml**

```yaml
server:
  port: 8080

spring:
  application:
    name: swaggerdocs

swaggerdocs:
  storage:
    path: ${user.home}/.swaggerdocs/storage
```

**Step 4: Verify project compiles**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 5: Commit**

```bash
git init
git add .
git commit -m "chore: initial project setup with Spring Boot"
```

---

## Task 2: Domain Models

**Files:**
- Create: `src/main/java/com/swaggerdocs/model/SwaggerSubmission.java`
- Create: `src/main/java/com/swaggerdocs/model/SwaggerMetadata.java`
- Create: `src/main/java/com/swaggerdocs/model/ValidationResult.java`
- Create: `src/main/java/com/swaggerdocs/model/BreakingChange.java`
- Create: `src/main/java/com/swaggerdocs/model/QualityScore.java`
- Create: `src/main/java/com/swaggerdocs/model/SwaggerEntry.java`

**Step 1: Create SwaggerSubmission (payload do POST)**

```java
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
```

**Step 2: Create SwaggerMetadata (armazenado junto com swagger)**

```java
package com.swaggerdocs.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
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
```

**Step 3: Create ValidationResult e componentes**

```java
package com.swaggerdocs.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ValidationResult {
    private String status; // ACCEPTED, ACCEPTED_WITH_WARNINGS
    private String version;
    private QualityScore quality;
    private List<BreakingChange> breakingChanges;
    private String viewUrl;
}
```

```java
package com.swaggerdocs.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class QualityScore {
    private int score;
    private List<QualityIssue> issues;

    @Data
    @Builder
    public static class QualityIssue {
        private String category;
        private String message;
        private String path;
    }
}
```

```java
package com.swaggerdocs.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BreakingChange {
    private ChangeType type;
    private String path;
    private String description;

    public enum ChangeType {
        ENDPOINT_REMOVED,
        METHOD_REMOVED,
        REQUIRED_PARAM_ADDED,
        RESPONSE_FIELD_REMOVED,
        TYPE_CHANGED,
        ENUM_VALUE_REMOVED
    }
}
```

**Step 4: Create SwaggerEntry (para listagem)**

```java
package com.swaggerdocs.model;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class SwaggerEntry {
    private String appName;
    private String team;
    private String version;
    private int qualityScore;
    private boolean hasBreakingChanges;
    private Instant updatedAt;
}
```

**Step 5: Verify compilation**

Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 6: Commit**

```bash
git add .
git commit -m "feat: add domain models for swagger submission and validation"
```

---

## Task 3: Git Storage Service

**Files:**
- Create: `src/main/java/com/swaggerdocs/service/GitStorageService.java`
- Create: `src/main/java/com/swaggerdocs/config/StorageConfig.java`
- Create: `src/test/java/com/swaggerdocs/service/GitStorageServiceTest.java`

**Step 1: Write failing test for GitStorageService**

```java
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
        service = new GitStorageService(tempDir.toString(), objectMapper);
        service.init();
    }

    @Test
    void shouldSaveAndRetrieveSwagger() throws Exception {
        // Given
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

        // When
        String version = service.save(appName, swagger, metadata);

        // Then
        assertThat(version).isNotNull().hasSize(7);

        var retrieved = service.getSwagger(appName);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().get("openapi").asText()).isEqualTo("3.0.0");
    }

    @Test
    void shouldListAllApps() throws Exception {
        // Given
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");

        service.save("app-a", swagger, SwaggerMetadata.builder()
                .appName("app-a").team("team-a").updatedAt(Instant.now()).build());
        service.save("app-b", swagger, SwaggerMetadata.builder()
                .appName("app-b").team("team-b").updatedAt(Instant.now()).build());

        // When
        var apps = service.listApps();

        // Then
        assertThat(apps).hasSize(2);
        assertThat(apps).extracting("appName").containsExactlyInAnyOrder("app-a", "app-b");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=GitStorageServiceTest -q`
Expected: Compilation error (class doesn't exist)

**Step 3: Create StorageConfig**

```java
package com.swaggerdocs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "swaggerdocs.storage")
public class StorageConfig {
    private String path;
}
```

**Step 4: Implement GitStorageService**

```java
package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swaggerdocs.model.SwaggerEntry;
import com.swaggerdocs.model.SwaggerMetadata;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
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
@RequiredArgsConstructor
public class GitStorageService {

    private final String storagePath;
    private final ObjectMapper objectMapper;

    private Git git;
    private Path storageDir;

    public GitStorageService(com.swaggerdocs.config.StorageConfig config, ObjectMapper objectMapper) {
        this.storagePath = config.getPath();
        this.objectMapper = objectMapper;
    }

    // Constructor for testing
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

            // Write swagger.json
            Path swaggerFile = appDir.resolve("swagger.json");
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(swaggerFile.toFile(), swagger);

            // Write metadata.json
            Path metadataFile = appDir.resolve("metadata.json");
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(metadataFile.toFile(), metadata);

            // Git add and commit
            git.add().addFilepattern(appName).call();

            String commitMessage = String.format("Update %s - %s",
                    appName,
                    metadata.getCommitHash() != null ? metadata.getCommitHash() : "manual");

            RevCommit commit = git.commit().setMessage(commitMessage).call();

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
            ObjectId commitId = git.getRepository().resolve(commitHash);
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
```

**Step 5: Run tests**

Run: `mvn test -Dtest=GitStorageServiceTest -q`
Expected: Tests pass

**Step 6: Commit**

```bash
git add .
git commit -m "feat: add Git storage service with versioning"
```

---

## Task 4: Validation Service (Quality Score)

**Files:**
- Create: `src/main/java/com/swaggerdocs/service/ValidationService.java`
- Create: `src/test/java/com/swaggerdocs/service/ValidationServiceTest.java`

**Step 1: Write failing test**

```java
package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.model.QualityScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceTest {

    private ValidationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ValidationService();
    }

    @Test
    void shouldScoreHighForCompleteSwagger() {
        // Given
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");

        var info = swagger.putObject("info");
        info.put("title", "Complete API");
        info.put("description", "A complete API with all fields");
        info.put("version", "1.0.0");
        info.put("contact", "team@example.com");

        var paths = swagger.putObject("paths");
        var endpoint = paths.putObject("/users");
        var get = endpoint.putObject("get");
        get.put("summary", "List users");
        get.put("description", "Returns all users");

        var responses = get.putObject("responses");
        responses.putObject("200").put("description", "Success");
        responses.putObject("400").put("description", "Bad request");
        responses.putObject("500").put("description", "Server error");

        // When
        QualityScore score = service.calculateQuality(swagger);

        // Then
        assertThat(score.getScore()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void shouldScoreLowForIncompleteSwagger() {
        // Given
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Minimal API");
        swagger.putObject("paths").putObject("/users").putObject("get");

        // When
        QualityScore score = service.calculateQuality(swagger);

        // Then
        assertThat(score.getScore()).isLessThan(50);
        assertThat(score.getIssues()).isNotEmpty();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=ValidationServiceTest -q`
Expected: Compilation error

**Step 3: Implement ValidationService**

```java
package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swaggerdocs.model.QualityScore;
import com.swaggerdocs.model.QualityScore.QualityIssue;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

    private static final int WEIGHT_DESCRIPTIONS = 25;
    private static final int WEIGHT_EXAMPLES = 20;
    private static final int WEIGHT_RESPONSES = 20;
    private static final int WEIGHT_SCHEMAS = 20;
    private static final int WEIGHT_METADATA = 15;

    public QualityScore calculateQuality(JsonNode swagger) {
        List<QualityIssue> issues = new ArrayList<>();

        int descriptionScore = checkDescriptions(swagger, issues);
        int examplesScore = checkExamples(swagger, issues);
        int responsesScore = checkResponses(swagger, issues);
        int schemasScore = checkSchemas(swagger, issues);
        int metadataScore = checkMetadata(swagger, issues);

        int totalScore =
                (descriptionScore * WEIGHT_DESCRIPTIONS +
                 examplesScore * WEIGHT_EXAMPLES +
                 responsesScore * WEIGHT_RESPONSES +
                 schemasScore * WEIGHT_SCHEMAS +
                 metadataScore * WEIGHT_METADATA) / 100;

        return QualityScore.builder()
                .score(totalScore)
                .issues(issues)
                .build();
    }

    private int checkDescriptions(JsonNode swagger, List<QualityIssue> issues) {
        int total = 0;
        int withDescription = 0;

        JsonNode paths = swagger.get("paths");
        if (paths != null) {
            var pathIterator = paths.fields();
            while (pathIterator.hasNext()) {
                var pathEntry = pathIterator.next();
                String path = pathEntry.getKey();
                JsonNode pathItem = pathEntry.getValue();

                var methodIterator = pathItem.fields();
                while (methodIterator.hasNext()) {
                    var methodEntry = methodIterator.next();
                    String method = methodEntry.getKey();
                    if (method.startsWith("$")) continue;

                    total++;
                    JsonNode operation = methodEntry.getValue();

                    if (hasText(operation.get("description")) || hasText(operation.get("summary"))) {
                        withDescription++;
                    } else {
                        issues.add(QualityIssue.builder()
                                .category("descriptions")
                                .message("Missing description")
                                .path(method.toUpperCase() + " " + path)
                                .build());
                    }
                }
            }
        }

        return total > 0 ? (withDescription * 100) / total : 0;
    }

    private int checkExamples(JsonNode swagger, List<QualityIssue> issues) {
        // Simplified: check if any examples exist in components/schemas
        JsonNode components = swagger.get("components");
        if (components == null) return 0;

        JsonNode schemas = components.get("schemas");
        if (schemas == null) return 50; // No schemas = neutral score

        int total = 0;
        int withExamples = 0;

        var schemaIterator = schemas.fields();
        while (schemaIterator.hasNext()) {
            var entry = schemaIterator.next();
            total++;
            if (entry.getValue().has("example") || entry.getValue().has("examples")) {
                withExamples++;
            }
        }

        if (total > 0 && withExamples < total) {
            issues.add(QualityIssue.builder()
                    .category("examples")
                    .message(String.format("%d of %d schemas missing examples", total - withExamples, total))
                    .path("components/schemas")
                    .build());
        }

        return total > 0 ? (withExamples * 100) / total : 50;
    }

    private int checkResponses(JsonNode swagger, List<QualityIssue> issues) {
        int total = 0;
        int withErrorResponses = 0;

        JsonNode paths = swagger.get("paths");
        if (paths == null) return 0;

        var pathIterator = paths.fields();
        while (pathIterator.hasNext()) {
            var pathEntry = pathIterator.next();
            String path = pathEntry.getKey();
            JsonNode pathItem = pathEntry.getValue();

            var methodIterator = pathItem.fields();
            while (methodIterator.hasNext()) {
                var methodEntry = methodIterator.next();
                String method = methodEntry.getKey();
                if (method.startsWith("$")) continue;

                total++;
                JsonNode responses = methodEntry.getValue().get("responses");

                if (responses != null) {
                    boolean has4xx = false;
                    boolean has5xx = false;

                    var respIterator = responses.fieldNames();
                    while (respIterator.hasNext()) {
                        String code = respIterator.next();
                        if (code.startsWith("4")) has4xx = true;
                        if (code.startsWith("5")) has5xx = true;
                    }

                    if (has4xx && has5xx) {
                        withErrorResponses++;
                    } else {
                        issues.add(QualityIssue.builder()
                                .category("responses")
                                .message("Missing error responses (4xx/5xx)")
                                .path(method.toUpperCase() + " " + path)
                                .build());
                    }
                }
            }
        }

        return total > 0 ? (withErrorResponses * 100) / total : 0;
    }

    private int checkSchemas(JsonNode swagger, List<QualityIssue> issues) {
        JsonNode components = swagger.get("components");
        if (components != null && components.has("schemas")) {
            return 100; // Has reusable schemas
        }

        // Check if inline schemas are used (less ideal)
        JsonNode paths = swagger.get("paths");
        if (paths != null && paths.toString().contains("\"type\"")) {
            issues.add(QualityIssue.builder()
                    .category("schemas")
                    .message("Consider using $ref for reusable schemas")
                    .path("paths")
                    .build());
            return 50;
        }

        return 0;
    }

    private int checkMetadata(JsonNode swagger, List<QualityIssue> issues) {
        int score = 0;
        JsonNode info = swagger.get("info");

        if (info == null) {
            issues.add(QualityIssue.builder()
                    .category("metadata")
                    .message("Missing info section")
                    .path("info")
                    .build());
            return 0;
        }

        if (hasText(info.get("title"))) score += 25;
        if (hasText(info.get("description"))) score += 25;
        if (hasText(info.get("version"))) score += 25;
        if (info.has("contact") || hasText(info.get("contact"))) score += 25;

        if (score < 100) {
            issues.add(QualityIssue.builder()
                    .category("metadata")
                    .message("Incomplete info section")
                    .path("info")
                    .build());
        }

        return score;
    }

    private boolean hasText(JsonNode node) {
        return node != null && !node.isNull() && !node.asText().isBlank();
    }
}
```

**Step 4: Run tests**

Run: `mvn test -Dtest=ValidationServiceTest -q`
Expected: Tests pass

**Step 5: Commit**

```bash
git add .
git commit -m "feat: add validation service for quality scoring"
```

---

## Task 5: Diff Service (Breaking Changes)

**Files:**
- Create: `src/main/java/com/swaggerdocs/service/DiffService.java`
- Create: `src/test/java/com/swaggerdocs/service/DiffServiceTest.java`

**Step 1: Write failing test**

```java
package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.model.BreakingChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiffServiceTest {

    private DiffService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new DiffService();
    }

    @Test
    void shouldDetectRemovedEndpoint() {
        // Given
        ObjectNode oldSwagger = createBaseSwagger();
        oldSwagger.with("paths").putObject("/users").putObject("get");
        oldSwagger.with("paths").putObject("/orders").putObject("get");

        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");
        // /orders removed

        // When
        List<BreakingChange> changes = service.findBreakingChanges(oldSwagger, newSwagger);

        // Then
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getType()).isEqualTo(BreakingChange.ChangeType.ENDPOINT_REMOVED);
        assertThat(changes.get(0).getPath()).contains("/orders");
    }

    @Test
    void shouldDetectRemovedMethod() {
        // Given
        ObjectNode oldSwagger = createBaseSwagger();
        var users = oldSwagger.with("paths").putObject("/users");
        users.putObject("get");
        users.putObject("post");

        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");
        // POST removed

        // When
        List<BreakingChange> changes = service.findBreakingChanges(oldSwagger, newSwagger);

        // Then
        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getType()).isEqualTo(BreakingChange.ChangeType.METHOD_REMOVED);
    }

    @Test
    void shouldNotReportAddedEndpoints() {
        // Given
        ObjectNode oldSwagger = createBaseSwagger();
        oldSwagger.with("paths").putObject("/users").putObject("get");

        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");
        newSwagger.with("paths").putObject("/orders").putObject("get"); // Added

        // When
        List<BreakingChange> changes = service.findBreakingChanges(oldSwagger, newSwagger);

        // Then
        assertThat(changes).isEmpty();
    }

    private ObjectNode createBaseSwagger() {
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Test API");
        swagger.putObject("paths");
        return swagger;
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=DiffServiceTest -q`
Expected: Compilation error

**Step 3: Implement DiffService**

```java
package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swaggerdocs.model.BreakingChange;
import com.swaggerdocs.model.BreakingChange.ChangeType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DiffService {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "head", "options"
    );

    public List<BreakingChange> findBreakingChanges(JsonNode oldSwagger, JsonNode newSwagger) {
        List<BreakingChange> changes = new ArrayList<>();

        if (oldSwagger == null) {
            return changes; // First version, no breaking changes
        }

        checkRemovedEndpoints(oldSwagger, newSwagger, changes);
        checkRemovedMethods(oldSwagger, newSwagger, changes);
        checkRemovedResponseFields(oldSwagger, newSwagger, changes);

        return changes;
    }

    private void checkRemovedEndpoints(JsonNode oldSwagger, JsonNode newSwagger, List<BreakingChange> changes) {
        JsonNode oldPaths = oldSwagger.get("paths");
        JsonNode newPaths = newSwagger.get("paths");

        if (oldPaths == null) return;
        if (newPaths == null) {
            // All endpoints removed
            oldPaths.fieldNames().forEachRemaining(path ->
                changes.add(BreakingChange.builder()
                        .type(ChangeType.ENDPOINT_REMOVED)
                        .path(path)
                        .description("Endpoint removed: " + path)
                        .build())
            );
            return;
        }

        Set<String> newPathSet = new HashSet<>();
        newPaths.fieldNames().forEachRemaining(newPathSet::add);

        oldPaths.fieldNames().forEachRemaining(path -> {
            if (!newPathSet.contains(path)) {
                changes.add(BreakingChange.builder()
                        .type(ChangeType.ENDPOINT_REMOVED)
                        .path(path)
                        .description("Endpoint removed: " + path)
                        .build());
            }
        });
    }

    private void checkRemovedMethods(JsonNode oldSwagger, JsonNode newSwagger, List<BreakingChange> changes) {
        JsonNode oldPaths = oldSwagger.get("paths");
        JsonNode newPaths = newSwagger.get("paths");

        if (oldPaths == null || newPaths == null) return;

        oldPaths.fieldNames().forEachRemaining(path -> {
            JsonNode oldPath = oldPaths.get(path);
            JsonNode newPath = newPaths.get(path);

            if (newPath == null) return; // Already reported as endpoint removed

            oldPath.fieldNames().forEachRemaining(method -> {
                if (!HTTP_METHODS.contains(method.toLowerCase())) return;

                if (!newPath.has(method)) {
                    changes.add(BreakingChange.builder()
                            .type(ChangeType.METHOD_REMOVED)
                            .path(method.toUpperCase() + " " + path)
                            .description("Method removed: " + method.toUpperCase() + " " + path)
                            .build());
                }
            });
        });
    }

    private void checkRemovedResponseFields(JsonNode oldSwagger, JsonNode newSwagger, List<BreakingChange> changes) {
        // Check components/schemas for removed properties
        JsonNode oldSchemas = getSchemas(oldSwagger);
        JsonNode newSchemas = getSchemas(newSwagger);

        if (oldSchemas == null || newSchemas == null) return;

        oldSchemas.fieldNames().forEachRemaining(schemaName -> {
            JsonNode oldSchema = oldSchemas.get(schemaName);
            JsonNode newSchema = newSchemas.get(schemaName);

            if (newSchema == null) {
                changes.add(BreakingChange.builder()
                        .type(ChangeType.RESPONSE_FIELD_REMOVED)
                        .path("components/schemas/" + schemaName)
                        .description("Schema removed: " + schemaName)
                        .build());
                return;
            }

            JsonNode oldProps = oldSchema.get("properties");
            JsonNode newProps = newSchema.get("properties");

            if (oldProps != null && newProps != null) {
                oldProps.fieldNames().forEachRemaining(prop -> {
                    if (!newProps.has(prop)) {
                        changes.add(BreakingChange.builder()
                                .type(ChangeType.RESPONSE_FIELD_REMOVED)
                                .path("components/schemas/" + schemaName + "/" + prop)
                                .description("Property removed from " + schemaName + ": " + prop)
                                .build());
                    }
                });
            }
        });
    }

    private JsonNode getSchemas(JsonNode swagger) {
        JsonNode components = swagger.get("components");
        return components != null ? components.get("schemas") : null;
    }
}
```

**Step 4: Run tests**

Run: `mvn test -Dtest=DiffServiceTest -q`
Expected: Tests pass

**Step 5: Commit**

```bash
git add .
git commit -m "feat: add diff service for breaking change detection"
```

---

## Task 6: Swagger Service (Orchestration)

**Files:**
- Create: `src/main/java/com/swaggerdocs/service/SwaggerService.java`
- Create: `src/test/java/com/swaggerdocs/service/SwaggerServiceTest.java`

**Step 1: Write failing test**

```java
package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.model.SwaggerSubmission;
import com.swaggerdocs.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SwaggerServiceTest {

    @Mock
    private GitStorageService gitStorageService;

    @Mock
    private ValidationService validationService;

    @Mock
    private DiffService diffService;

    private SwaggerService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new SwaggerService(gitStorageService, validationService, diffService, "http://localhost:8080");
    }

    @Test
    void shouldProcessNewSubmission() {
        // Given
        SwaggerSubmission submission = new SwaggerSubmission();
        submission.setAppName("test-api");
        submission.setTeam("test-team");

        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        submission.setSwagger(swagger);

        when(gitStorageService.getSwagger("test-api")).thenReturn(Optional.empty());
        when(gitStorageService.save(eq("test-api"), any(), any())).thenReturn("abc1234");
        when(validationService.calculateQuality(any())).thenReturn(
                com.swaggerdocs.model.QualityScore.builder().score(85).issues(Collections.emptyList()).build()
        );
        when(diffService.findBreakingChanges(any(), any())).thenReturn(Collections.emptyList());

        // When
        ValidationResult result = service.processSubmission(submission);

        // Then
        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        assertThat(result.getVersion()).isEqualTo("abc1234");
        assertThat(result.getQuality().getScore()).isEqualTo(85);
        assertThat(result.getViewUrl()).contains("test-api");
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SwaggerServiceTest -q`
Expected: Compilation error

**Step 3: Implement SwaggerService**

```java
package com.swaggerdocs.service;

import com.swaggerdocs.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SwaggerService {

    private final GitStorageService gitStorageService;
    private final ValidationService validationService;
    private final DiffService diffService;

    @Value("${swaggerdocs.base-url:http://localhost:8080}")
    private String baseUrl;

    // Constructor for testing
    public SwaggerService(GitStorageService gitStorageService,
                         ValidationService validationService,
                         DiffService diffService,
                         String baseUrl) {
        this.gitStorageService = gitStorageService;
        this.validationService = validationService;
        this.diffService = diffService;
        this.baseUrl = baseUrl;
    }

    public ValidationResult processSubmission(SwaggerSubmission submission) {
        String appName = submission.getAppName();

        // Get previous version for diff
        var previousSwagger = gitStorageService.getSwagger(appName).orElse(null);

        // Calculate quality score
        QualityScore quality = validationService.calculateQuality(submission.getSwagger());

        // Find breaking changes
        List<BreakingChange> breakingChanges = diffService.findBreakingChanges(
                previousSwagger,
                submission.getSwagger()
        );

        // Build metadata
        SwaggerMetadata metadata = SwaggerMetadata.builder()
                .appName(appName)
                .team(submission.getTeam())
                .environment(submission.getEnvironment())
                .commitHash(submission.getMetadata() != null ? submission.getMetadata().getCommitHash() : null)
                .branch(submission.getMetadata() != null ? submission.getMetadata().getBranch() : null)
                .pipelineUrl(submission.getMetadata() != null ? submission.getMetadata().getPipelineUrl() : null)
                .updatedAt(Instant.now())
                .build();

        // Save to Git
        String version = gitStorageService.save(appName, submission.getSwagger(), metadata);

        // Determine status
        String status = breakingChanges.isEmpty() ? "ACCEPTED" : "ACCEPTED_WITH_WARNINGS";

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
}
```

**Step 4: Create SwaggerInfo model**

```java
package com.swaggerdocs.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SwaggerInfo {
    private JsonNode swagger;
    private SwaggerMetadata metadata;
    private QualityScore quality;
    private List<String> versions;
}
```

**Step 5: Run tests**

Run: `mvn test -Dtest=SwaggerServiceTest -q`
Expected: Tests pass

**Step 6: Commit**

```bash
git add .
git commit -m "feat: add swagger service for orchestration"
```

---

## Task 7: REST Controller (API)

**Files:**
- Create: `src/main/java/com/swaggerdocs/controller/SwaggerController.java`
- Create: `src/test/java/com/swaggerdocs/controller/SwaggerControllerTest.java`

**Step 1: Write integration test**

```java
package com.swaggerdocs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.model.SwaggerSubmission;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SwaggerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAcceptSwaggerSubmission() throws Exception {
        SwaggerSubmission submission = new SwaggerSubmission();
        submission.setAppName("test-api");
        submission.setTeam("test-team");

        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Test API").put("version", "1.0.0");
        swagger.putObject("paths");
        submission.setSwagger(swagger);

        mockMvc.perform(post("/api/swaggers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.viewUrl").exists());
    }

    @Test
    void shouldListApps() throws Exception {
        mockMvc.perform(get("/api/swaggers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SwaggerControllerTest -q`
Expected: 404 (controller doesn't exist)

**Step 3: Implement SwaggerController**

```java
package com.swaggerdocs.controller;

import com.swaggerdocs.model.SwaggerEntry;
import com.swaggerdocs.model.SwaggerInfo;
import com.swaggerdocs.model.SwaggerSubmission;
import com.swaggerdocs.model.ValidationResult;
import com.swaggerdocs.service.SwaggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/swaggers")
@RequiredArgsConstructor
public class SwaggerController {

    private final SwaggerService swaggerService;

    @PostMapping
    public ResponseEntity<ValidationResult> submitSwagger(@RequestBody SwaggerSubmission submission) {
        ValidationResult result = swaggerService.processSubmission(submission);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<SwaggerEntry>> listApps() {
        return ResponseEntity.ok(swaggerService.listApps());
    }

    @GetMapping("/{appName}")
    public ResponseEntity<SwaggerInfo> getApp(@PathVariable String appName) {
        return swaggerService.getApp(appName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{appName}/raw")
    public ResponseEntity<?> getRawSwagger(@PathVariable String appName) {
        return swaggerService.getApp(appName)
                .map(info -> ResponseEntity.ok(info.getSwagger()))
                .orElse(ResponseEntity.notFound().build());
    }
}
```

**Step 4: Run tests**

Run: `mvn test -Dtest=SwaggerControllerTest -q`
Expected: Tests pass

**Step 5: Commit**

```bash
git add .
git commit -m "feat: add REST controller for swagger API"
```

---

## Task 8: Portal Controller (Frontend)

**Files:**
- Create: `src/main/java/com/swaggerdocs/controller/PortalController.java`
- Create: `src/main/resources/templates/index.html`
- Create: `src/main/resources/templates/docs.html`
- Create: `src/main/resources/static/css/style.css`

**Step 1: Create PortalController**

```java
package com.swaggerdocs.controller;

import com.swaggerdocs.service.SwaggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PortalController {

    private final SwaggerService swaggerService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("apps", swaggerService.listApps());
        return "index";
    }

    @GetMapping("/docs/{appName}")
    public String docs(@PathVariable String appName,
                      @RequestParam(defaultValue = "swagger-ui") String view,
                      Model model) {
        return swaggerService.getApp(appName)
                .map(info -> {
                    model.addAttribute("appName", appName);
                    model.addAttribute("info", info);
                    model.addAttribute("view", view);
                    return "docs";
                })
                .orElse("redirect:/");
    }
}
```

**Step 2: Create index.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SwaggerDocs Portal</title>
    <link rel="stylesheet" th:href="@{/css/style.css}">
</head>
<body>
    <header>
        <h1>SwaggerDocs Portal</h1>
    </header>

    <main>
        <section class="apps-grid">
            <div th:if="${#lists.isEmpty(apps)}" class="empty-state">
                <p>Nenhuma API registrada ainda.</p>
                <p>Configure seu CI para enviar Swaggers para este portal.</p>
            </div>

            <article th:each="app : ${apps}" class="app-card">
                <h2 th:text="${app.appName}">app-name</h2>
                <p class="team" th:text="'Team: ' + ${app.team}">Team: team-name</p>
                <p class="version" th:text="'v' + ${app.version}">v1.0.0</p>

                <div class="score" th:classappend="${app.qualityScore >= 70 ? 'good' : (app.qualityScore >= 50 ? 'warning' : 'bad')}">
                    <span th:text="'Score: ' + ${app.qualityScore}">Score: 85</span>
                </div>

                <span th:if="${app.hasBreakingChanges}" class="badge breaking">Breaking Changes!</span>

                <a th:href="@{/docs/{name}(name=${app.appName})}" class="btn">Ver Documentação</a>
            </article>
        </section>
    </main>
</body>
</html>
```

**Step 3: Create docs.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${appName} + ' - SwaggerDocs'">API - SwaggerDocs</title>
    <link rel="stylesheet" th:href="@{/css/style.css}">
</head>
<body>
    <header>
        <a href="/" class="back">← Voltar</a>
        <h1 th:text="${appName}">app-name</h1>

        <nav class="view-toggle">
            <a th:href="@{/docs/{name}(name=${appName}, view='swagger-ui')}"
               th:classappend="${view == 'swagger-ui' ? 'active' : ''}">Swagger UI</a>
            <a th:href="@{/docs/{name}(name=${appName}, view='redoc')}"
               th:classappend="${view == 'redoc' ? 'active' : ''}">Redoc</a>
            <a th:href="@{/api/swaggers/{name}/raw(name=${appName})}" target="_blank">Raw JSON</a>
        </nav>
    </header>

    <aside class="metadata">
        <p><strong>Team:</strong> <span th:text="${info.metadata?.team}">-</span></p>
        <p><strong>Atualizado:</strong> <span th:text="${info.metadata?.updatedAt}">-</span></p>
        <p><strong>Score:</strong> <span th:text="${info.quality?.score}">-</span>/100</p>

        <div th:if="${!info.quality?.issues.isEmpty()}" class="issues">
            <h4>Problemas de Qualidade:</h4>
            <ul>
                <li th:each="issue : ${info.quality.issues}" th:text="${issue.message}">Issue</li>
            </ul>
        </div>
    </aside>

    <main class="docs-container">
        <!-- Swagger UI -->
        <div th:if="${view == 'swagger-ui'}" id="swagger-ui"></div>

        <!-- Redoc -->
        <div th:if="${view == 'redoc'}">
            <redoc spec-url="/api/swaggers/{appName}/raw" th:attr="spec-url=@{/api/swaggers/{name}/raw(name=${appName})}"></redoc>
        </div>
    </main>

    <!-- Swagger UI Assets -->
    <th:block th:if="${view == 'swagger-ui'}">
        <link rel="stylesheet" th:href="@{/webjars/swagger-ui/swagger-ui.css}">
        <script th:src="@{/webjars/swagger-ui/swagger-ui-bundle.js}"></script>
        <script th:inline="javascript">
            window.onload = function() {
                SwaggerUIBundle({
                    url: /*[[@{/api/swaggers/{name}/raw(name=${appName})}]]*/ '/api/swaggers/app/raw',
                    dom_id: '#swagger-ui',
                    presets: [SwaggerUIBundle.presets.apis],
                    layout: "BaseLayout"
                });
            };
        </script>
    </th:block>

    <!-- Redoc Assets -->
    <th:block th:if="${view == 'redoc'}">
        <script src="https://cdn.redoc.ly/redoc/latest/bundles/redoc.standalone.js"></script>
    </th:block>
</body>
</html>
```

**Step 4: Create style.css**

```css
* {
    box-sizing: border-box;
    margin: 0;
    padding: 0;
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: #f5f5f5;
    color: #333;
}

header {
    background: #2c3e50;
    color: white;
    padding: 1rem 2rem;
    display: flex;
    align-items: center;
    gap: 2rem;
}

header h1 {
    font-size: 1.5rem;
}

header .back {
    color: white;
    text-decoration: none;
}

.view-toggle {
    margin-left: auto;
    display: flex;
    gap: 1rem;
}

.view-toggle a {
    color: rgba(255,255,255,0.7);
    text-decoration: none;
    padding: 0.5rem 1rem;
    border-radius: 4px;
}

.view-toggle a.active {
    background: rgba(255,255,255,0.2);
    color: white;
}

main {
    padding: 2rem;
    max-width: 1400px;
    margin: 0 auto;
}

.apps-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 1.5rem;
}

.app-card {
    background: white;
    border-radius: 8px;
    padding: 1.5rem;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.app-card h2 {
    margin-bottom: 0.5rem;
    color: #2c3e50;
}

.app-card .team {
    color: #666;
    font-size: 0.9rem;
}

.app-card .version {
    color: #888;
    font-size: 0.85rem;
    margin-bottom: 1rem;
}

.score {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 20px;
    font-size: 0.85rem;
    font-weight: 500;
}

.score.good { background: #d4edda; color: #155724; }
.score.warning { background: #fff3cd; color: #856404; }
.score.bad { background: #f8d7da; color: #721c24; }

.badge.breaking {
    background: #dc3545;
    color: white;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
    margin-left: 0.5rem;
}

.btn {
    display: block;
    margin-top: 1rem;
    padding: 0.75rem 1rem;
    background: #3498db;
    color: white;
    text-decoration: none;
    border-radius: 4px;
    text-align: center;
}

.btn:hover {
    background: #2980b9;
}

.empty-state {
    text-align: center;
    padding: 4rem 2rem;
    color: #666;
}

.metadata {
    background: white;
    padding: 1rem 2rem;
    border-bottom: 1px solid #eee;
    display: flex;
    gap: 2rem;
    flex-wrap: wrap;
}

.metadata p {
    font-size: 0.9rem;
}

.docs-container {
    padding: 0;
}

#swagger-ui {
    background: white;
}
```

**Step 5: Verify application runs**

Run: `mvn spring-boot:run`
Expected: Application starts on port 8080

**Step 6: Commit**

```bash
git add .
git commit -m "feat: add portal frontend with Swagger UI and Redoc"
```

---

## Task 9: Add Jackson DateTime Support

**Files:**
- Modify: `pom.xml` (add dependency)
- Create: `src/main/java/com/swaggerdocs/config/JacksonConfig.java`

**Step 1: Add Jackson JSR310 dependency to pom.xml**

Add to dependencies section:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

**Step 2: Create JacksonConfig**

```java
package com.swaggerdocs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

**Step 3: Run all tests**

Run: `mvn test -q`
Expected: All tests pass

**Step 4: Commit**

```bash
git add .
git commit -m "chore: add Jackson datetime support"
```

---

## Task 10: Final Integration Test

**Files:**
- Create: `src/test/java/com/swaggerdocs/SwaggerDocsIntegrationTest.java`

**Step 1: Write end-to-end test**

```java
package com.swaggerdocs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.model.SwaggerSubmission;
import com.swaggerdocs.model.ValidationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerDocsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullWorkflow() {
        // 1. Submit a swagger
        SwaggerSubmission submission = createSubmission("integration-test-api");

        var response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/swaggers",
                submission,
                ValidationResult.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("ACCEPTED");

        // 2. List apps
        var listResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/swaggers",
                Object[].class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSizeGreaterThanOrEqualTo(1);

        // 3. Get specific app
        var getResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/swaggers/integration-test-api",
                Object.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 4. Access portal
        var portalResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/",
                String.class
        );

        assertThat(portalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(portalResponse.getBody()).contains("SwaggerDocs Portal");
    }

    private SwaggerSubmission createSubmission(String appName) {
        SwaggerSubmission submission = new SwaggerSubmission();
        submission.setAppName(appName);
        submission.setTeam("integration-test");
        submission.setEnvironment("test");

        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");

        var info = swagger.putObject("info");
        info.put("title", "Integration Test API");
        info.put("description", "API for testing");
        info.put("version", "1.0.0");

        var paths = swagger.putObject("paths");
        var users = paths.putObject("/users");
        var get = users.putObject("get");
        get.put("summary", "List users");
        get.put("description", "Returns all users");

        var responses = get.putObject("responses");
        responses.putObject("200").put("description", "Success");
        responses.putObject("400").put("description", "Bad request");
        responses.putObject("500").put("description", "Server error");

        submission.setSwagger(swagger);

        var metadata = new SwaggerSubmission.SubmissionMetadata();
        metadata.setCommitHash("test123");
        metadata.setBranch("main");
        submission.setMetadata(metadata);

        return submission;
    }
}
```

**Step 2: Run all tests**

Run: `mvn test -q`
Expected: All tests pass

**Step 3: Commit**

```bash
git add .
git commit -m "test: add integration test for full workflow"
```

---

## Summary

After completing all tasks, you will have:

1. **Spring Boot application** with REST API for receiving Swaggers
2. **Git-based storage** with version history
3. **Quality validation** with scoring
4. **Breaking change detection**
5. **Web portal** with Swagger UI and Redoc

**To run:**
```bash
mvn spring-boot:run
```

**To test CI integration:**
```bash
curl -X POST http://localhost:8080/api/swaggers \
  -H "Content-Type: application/json" \
  -d '{
    "appName": "my-api",
    "team": "my-team",
    "swagger": {"openapi": "3.0.0", "info": {"title": "My API", "version": "1.0.0"}, "paths": {}}
  }'
```
