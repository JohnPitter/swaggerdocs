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
        assertThat(response.getBody().getVersion()).isNotNull();
        assertThat(response.getBody().getQuality()).isNotNull();

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

        // 4. Get raw swagger
        var rawResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/swaggers/integration-test-api/raw",
                Object.class
        );

        assertThat(rawResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. Access portal
        var portalResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/",
                String.class
        );

        assertThat(portalResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(portalResponse.getBody()).contains("SwaggerDocs Portal");

        // 6. Access docs page
        var docsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/docs/integration-test-api",
                String.class
        );

        assertThat(docsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docsResponse.getBody()).contains("integration-test-api");
    }

    @Test
    void shouldDetectBreakingChangesOnUpdate() {
        String appName = "breaking-test-api";

        // Submit initial version
        SwaggerSubmission v1 = createSubmission(appName);
        ((ObjectNode) v1.getSwagger().get("paths")).putObject("/users").putObject("get");
        ((ObjectNode) v1.getSwagger().get("paths")).putObject("/orders").putObject("get");

        var v1Response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/swaggers",
                v1,
                ValidationResult.class
        );
        assertThat(v1Response.getBody().getStatus()).isEqualTo("ACCEPTED");

        // Submit version with removed endpoint
        SwaggerSubmission v2 = createSubmission(appName);
        ((ObjectNode) v2.getSwagger().get("paths")).putObject("/users").putObject("get");
        // /orders removed

        var v2Response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/swaggers",
                v2,
                ValidationResult.class
        );

        assertThat(v2Response.getBody().getStatus()).isEqualTo("ACCEPTED_WITH_WARNINGS");
        assertThat(v2Response.getBody().getBreakingChanges()).isNotEmpty();
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

        swagger.putObject("paths");

        submission.setSwagger(swagger);

        var metadata = new SwaggerSubmission.SubmissionMetadata();
        metadata.setCommitHash("test123");
        metadata.setBranch("main");
        submission.setMetadata(metadata);

        return submission;
    }
}
