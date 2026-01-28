package com.swaggerdocs.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiVersionDetectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDetectSwagger20() throws Exception {
        String json = """
            {
                "swagger": "2.0",
                "info": { "title": "Test", "version": "1.0" },
                "paths": {}
            }
            """;
        JsonNode spec = objectMapper.readTree(json);

        assertEquals(OpenApiVersionDetector.SpecVersion.SWAGGER_2_0, OpenApiVersionDetector.detectVersion(spec));
        assertTrue(OpenApiVersionDetector.isSwagger2(spec));
        assertFalse(OpenApiVersionDetector.isOpenApi3(spec));
        assertEquals("Swagger 2.0", OpenApiVersionDetector.getVersionString(spec));
    }

    @Test
    void shouldDetectOpenApi30() throws Exception {
        String json = """
            {
                "openapi": "3.0.0",
                "info": { "title": "Test", "version": "1.0" },
                "paths": {}
            }
            """;
        JsonNode spec = objectMapper.readTree(json);

        assertEquals(OpenApiVersionDetector.SpecVersion.OPENAPI_3_0, OpenApiVersionDetector.detectVersion(spec));
        assertFalse(OpenApiVersionDetector.isSwagger2(spec));
        assertTrue(OpenApiVersionDetector.isOpenApi3(spec));
        assertEquals("OpenAPI 3.0.0", OpenApiVersionDetector.getVersionString(spec));
    }

    @Test
    void shouldDetectOpenApi31() throws Exception {
        String json = """
            {
                "openapi": "3.1.0",
                "info": { "title": "Test", "version": "1.0" },
                "paths": {}
            }
            """;
        JsonNode spec = objectMapper.readTree(json);

        assertEquals(OpenApiVersionDetector.SpecVersion.OPENAPI_3_1, OpenApiVersionDetector.detectVersion(spec));
        assertFalse(OpenApiVersionDetector.isSwagger2(spec));
        assertTrue(OpenApiVersionDetector.isOpenApi3(spec));
        assertEquals("OpenAPI 3.1.0", OpenApiVersionDetector.getVersionString(spec));
    }

    @Test
    void shouldReturnUnknownForInvalidSpec() throws Exception {
        String json = """
            {
                "info": { "title": "Test" }
            }
            """;
        JsonNode spec = objectMapper.readTree(json);

        assertEquals(OpenApiVersionDetector.SpecVersion.UNKNOWN, OpenApiVersionDetector.detectVersion(spec));
        assertFalse(OpenApiVersionDetector.isSwagger2(spec));
        assertFalse(OpenApiVersionDetector.isOpenApi3(spec));
        assertEquals("unknown", OpenApiVersionDetector.getVersionString(spec));
    }

    @Test
    void shouldGetSchemasFromSwagger20() throws Exception {
        String json = """
            {
                "swagger": "2.0",
                "definitions": {
                    "User": { "type": "object" }
                }
            }
            """;
        JsonNode spec = objectMapper.readTree(json);

        JsonNode schemas = OpenApiVersionDetector.getSchemas(spec);
        assertNotNull(schemas);
        assertTrue(schemas.has("User"));
        assertEquals("definitions/", OpenApiVersionDetector.getSchemaPathPrefix(spec));
    }

    @Test
    void shouldGetSchemasFromOpenApi3() throws Exception {
        String json = """
            {
                "openapi": "3.0.0",
                "components": {
                    "schemas": {
                        "User": { "type": "object" }
                    }
                }
            }
            """;
        JsonNode spec = objectMapper.readTree(json);

        JsonNode schemas = OpenApiVersionDetector.getSchemas(spec);
        assertNotNull(schemas);
        assertTrue(schemas.has("User"));
        assertEquals("components/schemas/", OpenApiVersionDetector.getSchemaPathPrefix(spec));
    }

    @Test
    void shouldReturnNullSchemasWhenMissing() throws Exception {
        String json = """
            {
                "openapi": "3.0.0",
                "paths": {}
            }
            """;
        JsonNode spec = objectMapper.readTree(json);

        assertNull(OpenApiVersionDetector.getSchemas(spec));
    }

    @Test
    void shouldHandleNullSpec() {
        assertEquals(OpenApiVersionDetector.SpecVersion.UNKNOWN, OpenApiVersionDetector.detectVersion(null));
        assertFalse(OpenApiVersionDetector.isSwagger2(null));
        assertFalse(OpenApiVersionDetector.isOpenApi3(null));
        assertEquals("unknown", OpenApiVersionDetector.getVersionString(null));
        assertNull(OpenApiVersionDetector.getSchemas(null));
    }
}
