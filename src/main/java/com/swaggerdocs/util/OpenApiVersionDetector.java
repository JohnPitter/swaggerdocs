package com.swaggerdocs.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OpenApiVersionDetector {

    public enum SpecVersion {
        SWAGGER_2_0,
        OPENAPI_3_0,
        OPENAPI_3_1,
        UNKNOWN
    }

    public SpecVersion detectVersion(JsonNode spec) {
        if (spec == null) {
            return SpecVersion.UNKNOWN;
        }

        // Check for OpenAPI 3.x
        JsonNode openapi = spec.get("openapi");
        if (openapi != null && openapi.isTextual()) {
            String version = openapi.asText();
            if (version.startsWith("3.1")) {
                return SpecVersion.OPENAPI_3_1;
            } else if (version.startsWith("3.0") || version.startsWith("3.")) {
                return SpecVersion.OPENAPI_3_0;
            }
        }

        // Check for Swagger 2.0
        JsonNode swagger = spec.get("swagger");
        if (swagger != null && swagger.isTextual()) {
            String version = swagger.asText();
            if (version.startsWith("2.")) {
                return SpecVersion.SWAGGER_2_0;
            }
        }

        return SpecVersion.UNKNOWN;
    }

    public boolean isSwagger2(JsonNode spec) {
        return detectVersion(spec) == SpecVersion.SWAGGER_2_0;
    }

    public boolean isOpenApi3(JsonNode spec) {
        SpecVersion version = detectVersion(spec);
        return version == SpecVersion.OPENAPI_3_0 || version == SpecVersion.OPENAPI_3_1;
    }

    public String getVersionString(JsonNode spec) {
        if (spec == null) return "unknown";

        JsonNode openapi = spec.get("openapi");
        if (openapi != null && openapi.isTextual()) {
            return "OpenAPI " + openapi.asText();
        }

        JsonNode swagger = spec.get("swagger");
        if (swagger != null && swagger.isTextual()) {
            return "Swagger " + swagger.asText();
        }

        return "unknown";
    }

    /**
     * Gets the schemas node regardless of spec version.
     * - Swagger 2.0: definitions
     * - OpenAPI 3.x: components/schemas
     */
    public JsonNode getSchemas(JsonNode spec) {
        if (spec == null) return null;

        if (isSwagger2(spec)) {
            return spec.get("definitions");
        } else {
            JsonNode components = spec.get("components");
            return components != null ? components.get("schemas") : null;
        }
    }

    /**
     * Gets the schema path prefix for the spec version.
     */
    public String getSchemaPathPrefix(JsonNode spec) {
        if (isSwagger2(spec)) {
            return "definitions/";
        }
        return "components/schemas/";
    }
}
