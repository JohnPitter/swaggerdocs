package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swaggerdocs.model.BreakingChange;
import com.swaggerdocs.model.BreakingChange.ChangeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DiffService {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "head", "options"
    );

    public List<BreakingChange> findBreakingChanges(JsonNode oldSwagger, JsonNode newSwagger) {
        List<BreakingChange> changes = new ArrayList<>();

        if (oldSwagger == null) {
            log.debug("No previous version, skipping breaking change detection");
            return changes;
        }

        checkRemovedEndpoints(oldSwagger, newSwagger, changes);
        checkRemovedMethods(oldSwagger, newSwagger, changes);
        checkRemovedResponseFields(oldSwagger, newSwagger, changes);

        if (!changes.isEmpty()) {
            log.warn("Detected {} breaking changes", changes.size());
        }

        return changes;
    }

    private void checkRemovedEndpoints(JsonNode oldSwagger, JsonNode newSwagger, List<BreakingChange> changes) {
        JsonNode oldPaths = oldSwagger.get("paths");
        JsonNode newPaths = newSwagger.get("paths");

        if (oldPaths == null) return;
        if (newPaths == null) {
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

            if (newPath == null) return;

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
