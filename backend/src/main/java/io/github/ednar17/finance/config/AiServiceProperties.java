package io.github.ednar17.finance.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-service")
public record AiServiceProperties(String url, Duration readTimeout) {

    public AiServiceProperties {
        if (readTimeout == null) {
            readTimeout = Duration.ofMinutes(5);
        }
    }
}
