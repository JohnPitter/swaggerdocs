package com.swaggerdocs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakingChange {
    private ChangeType type;
    private String path;
    private String description;

    public enum ChangeType {
        ENDPOINT_REMOVED,
        METHOD_REMOVED,
        REQUIRED_PARAM_ADDED,
        RESPONSE_FIELD_REMOVED,
        TYPE_CHANGED,
        ENUM_VALUE_REMOVED
    }
}
