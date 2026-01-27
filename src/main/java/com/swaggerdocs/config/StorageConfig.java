package com.swaggerdocs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "swaggerdocs.storage")
public class StorageConfig {
    private String path;
}
