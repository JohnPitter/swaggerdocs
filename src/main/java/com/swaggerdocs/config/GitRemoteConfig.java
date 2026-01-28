package com.swaggerdocs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "swaggerdocs.git.remote")
public class GitRemoteConfig {
    private boolean enabled = false;
    private String url;
    private String branch = "main";
    private String token;
    private RetryConfig retry = new RetryConfig();

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private long delayMs = 1000;
    }

    public boolean isConfigured() {
        return enabled && url != null && !url.isBlank() && token != null && !token.isBlank();
    }
}
