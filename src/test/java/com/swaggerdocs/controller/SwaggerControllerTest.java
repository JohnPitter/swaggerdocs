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

    @Test
    void shouldReturn404ForNonExistentApp() throws Exception {
        mockMvc.perform(get("/api/swaggers/non-existent-app"))
                .andExpect(status().isNotFound());
    }
}
