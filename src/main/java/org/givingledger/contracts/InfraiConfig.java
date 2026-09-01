package org.givingledger.contracts;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("infrai")
public record InfraiConfig(URI baseUrl, String apiKey, int maxAttempts) {
    public InfraiConfig {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("INFRAI_API_KEY is required");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("infrai.max-attempts must be positive");
        }
    }
}
