package com.swaggerdocs.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwaggerInfo {
    private JsonNode swagger;
    private SwaggerMetadata metadata;
    private QualityScore quality;
    private List<String> versions;
}
