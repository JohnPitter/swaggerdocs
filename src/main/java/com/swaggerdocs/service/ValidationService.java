package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.swaggerdocs.model.QualityScore;
import com.swaggerdocs.model.QualityScore.QualityIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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

        log.debug("Quality score calculated: {} (desc={}, ex={}, resp={}, schema={}, meta={})",
                totalScore, descriptionScore, examplesScore, responsesScore, schemasScore, metadataScore);

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
                    if (method.startsWith("$") || method.equals("parameters")) continue;

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
        JsonNode components = swagger.get("components");
        if (components == null) return 0;

        JsonNode schemas = components.get("schemas");
        if (schemas == null) return 50;

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
                if (method.startsWith("$") || method.equals("parameters")) continue;

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
            return 100;
        }

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
