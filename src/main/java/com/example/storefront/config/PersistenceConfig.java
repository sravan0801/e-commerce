package com.example.storefront.config;

import java.time.Instant;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * on {@link com.example.storefront.common.BaseEntity} are filled in automatically.
 * A UTC {@link Instant} provider keeps timestamps timezone-agnostic.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider")
public class PersistenceConfig {

    @Bean
    public DateTimeProvider utcDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
