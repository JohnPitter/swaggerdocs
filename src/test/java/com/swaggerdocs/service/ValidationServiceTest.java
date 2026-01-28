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
        var ok = responses.putObject("200");
        ok.put("description", "Success");
        ok.putObject("content").putObject("application/json")
            .putObject("schema").put("type", "array");
        ok.putObject("examples").putObject("default").put("value", "[]");

        responses.putObject("400").put("description", "Bad request");
        responses.putObject("500").put("description", "Server error");

        // Add request body with schema and example
        var requestBody = get.putObject("requestBody");
        var content = requestBody.putObject("content").putObject("application/json");
        content.putObject("schema").put("type", "object");
        content.putObject("example").put("name", "John");

        // Add components with schemas
        var components = swagger.putObject("components");
        var schemas = components.putObject("schemas");
        var userSchema = schemas.putObject("User");
        userSchema.put("type", "object");
        userSchema.put("description", "User entity");

        QualityScore score = service.calculateQuality(swagger);

        assertThat(score.getScore()).isGreaterThanOrEqualTo(70);
    }

    @Test
    void shouldScoreLowForIncompleteSwagger() {
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Minimal API");
        swagger.putObject("paths").putObject("/users").putObject("get");

        QualityScore score = service.calculateQuality(swagger);

        assertThat(score.getScore()).isLessThan(50);
        assertThat(score.getIssues()).isNotEmpty();
    }

    @Test
    void shouldDetectMissingDescriptions() {
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Test");
        swagger.putObject("paths").putObject("/test").putObject("get");

        QualityScore score = service.calculateQuality(swagger);

        assertThat(score.getIssues())
                .anyMatch(i -> i.getCategory().equals("descriptions"));
    }

    @Test
    void shouldDetectMissingErrorResponses() {
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Test");

        var get = swagger.putObject("paths").putObject("/test").putObject("get");
        get.put("summary", "Test");
        get.putObject("responses").putObject("200").put("description", "OK");

        QualityScore score = service.calculateQuality(swagger);

        assertThat(score.getIssues())
                .anyMatch(i -> i.getCategory().equals("responses"));
    }
}
