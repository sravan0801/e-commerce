package com.example.storefront.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of the {@code app.*} configuration tree. Bound at startup via
 * {@code @ConfigurationPropertiesScan} on the main application class.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Seed seed) {

    public record Jwt(String secret, String issuer, Duration accessTokenTtl) {
    }

    public record Seed(boolean enabled) {
    }
}
