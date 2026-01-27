package com.swaggerdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swaggerdocs.model.QualityScore;
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
        SwaggerSubmission submission = new SwaggerSubmission();
        submission.setAppName("test-api");
        submission.setTeam("test-team");

        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        submission.setSwagger(swagger);

        when(gitStorageService.getSwagger("test-api")).thenReturn(Optional.empty());
        when(gitStorageService.save(eq("test-api"), any(), any())).thenReturn("abc1234");
        when(validationService.calculateQuality(any())).thenReturn(
                QualityScore.builder().score(85).issues(Collections.emptyList()).build()
        );
        when(diffService.findBreakingChanges(any(), any())).thenReturn(Collections.emptyList());

        ValidationResult result = service.processSubmission(submission);

        assertThat(result.getStatus()).isEqualTo("ACCEPTED");
        assertThat(result.getVersion()).isEqualTo("abc1234");
        assertThat(result.getQuality().getScore()).isEqualTo(85);
        assertThat(result.getViewUrl()).contains("test-api");
    }

    @Test
    void shouldReturnWarningStatusWhenBreakingChangesDetected() {
        SwaggerSubmission submission = new SwaggerSubmission();
        submission.setAppName("breaking-api");
        submission.setTeam("test-team");

        ObjectNode swagger = objectMapper.createObjectNode();
        swagger.put("openapi", "3.0.0");
        submission.setSwagger(swagger);

        ObjectNode oldSwagger = objectMapper.createObjectNode();
        when(gitStorageService.getSwagger("breaking-api")).thenReturn(Optional.of(oldSwagger));
        when(gitStorageService.save(eq("breaking-api"), any(), any())).thenReturn("def5678");
        when(validationService.calculateQuality(any())).thenReturn(
                QualityScore.builder().score(70).issues(Collections.emptyList()).build()
        );
        when(diffService.findBreakingChanges(any(), any())).thenReturn(
                Collections.singletonList(
                        com.swaggerdocs.model.BreakingChange.builder()
                                .type(com.swaggerdocs.model.BreakingChange.ChangeType.ENDPOINT_REMOVED)
                                .path("/removed")
                                .build()
                )
        );

        ValidationResult result = service.processSubmission(submission);

        assertThat(result.getStatus()).isEqualTo("ACCEPTED_WITH_WARNINGS");
        assertThat(result.getBreakingChanges()).hasSize(1);
    }
}
