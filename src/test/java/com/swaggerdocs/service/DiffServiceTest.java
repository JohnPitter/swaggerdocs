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
        ObjectNode oldSwagger = createBaseSwagger();
        oldSwagger.with("paths").putObject("/users").putObject("get");
        oldSwagger.with("paths").putObject("/orders").putObject("get");

        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");

        List<BreakingChange> changes = service.findBreakingChanges(oldSwagger, newSwagger);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getType()).isEqualTo(BreakingChange.ChangeType.ENDPOINT_REMOVED);
        assertThat(changes.get(0).getPath()).contains("/orders");
    }

    @Test
    void shouldDetectRemovedMethod() {
        ObjectNode oldSwagger = createBaseSwagger();
        var users = oldSwagger.with("paths").putObject("/users");
        users.putObject("get");
        users.putObject("post");

        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");

        List<BreakingChange> changes = service.findBreakingChanges(oldSwagger, newSwagger);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getType()).isEqualTo(BreakingChange.ChangeType.METHOD_REMOVED);
    }

    @Test
    void shouldNotReportAddedEndpoints() {
        ObjectNode oldSwagger = createBaseSwagger();
        oldSwagger.with("paths").putObject("/users").putObject("get");

        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");
        newSwagger.with("paths").putObject("/orders").putObject("get");

        List<BreakingChange> changes = service.findBreakingChanges(oldSwagger, newSwagger);

        assertThat(changes).isEmpty();
    }

    @Test
    void shouldNotReportBreakingChangesForFirstVersion() {
        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");

        List<BreakingChange> changes = service.findBreakingChanges(null, newSwagger);

        assertThat(changes).isEmpty();
    }

    @Test
    void shouldDetectRemovedSchemaProperty() {
        ObjectNode oldSwagger = createBaseSwagger();
        oldSwagger.with("paths").putObject("/users").putObject("get");
        var oldSchema = oldSwagger.with("components").with("schemas").putObject("User");
        oldSchema.with("properties").putObject("id").put("type", "string");
        oldSchema.with("properties").putObject("name").put("type", "string");

        ObjectNode newSwagger = createBaseSwagger();
        newSwagger.with("paths").putObject("/users").putObject("get");
        var newSchema = newSwagger.with("components").with("schemas").putObject("User");
        newSchema.with("properties").putObject("id").put("type", "string");

        List<BreakingChange> changes = service.findBreakingChanges(oldSwagger, newSwagger);

        assertThat(changes).hasSize(1);
        assertThat(changes.get(0).getType()).isEqualTo(BreakingChange.ChangeType.RESPONSE_FIELD_REMOVED);
        assertThat(changes.get(0).getPath()).contains("name");
    }

    private ObjectNode createBaseSwagger() {
        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        swagger.putObject("info").put("title", "Test API");
        swagger.putObject("paths");
        return swagger;
    }
}
