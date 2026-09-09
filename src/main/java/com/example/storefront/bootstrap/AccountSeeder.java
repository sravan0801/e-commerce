package com.example.storefront.bootstrap;

import com.example.storefront.security.AppUser;
import com.example.storefront.security.AppUserRepository;
import com.example.storefront.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates two demo accounts on startup when {@code app.seed.enabled=true}:
 *   admin@storefront.test / admin12345   (ROLE_ADMIN + ROLE_CUSTOMER)
 *   demo@storefront.test  / demo12345    (ROLE_CUSTOMER)
 * Idempotent -- skips accounts that already exist.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class AccountSeeder {

    private static final Logger log = LoggerFactory.getLogger(AccountSeeder.class);

    @Bean
    ApplicationRunner seedAccounts(AppUserRepository users, PasswordEncoder encoder) {
        return args -> {
            ensure(users, encoder, "admin@storefront.test", "admin12345", "Store Admin",
                    Role.ADMIN, Role.CUSTOMER);
            ensure(users, encoder, "demo@storefront.test", "demo12345", "Demo Customer",
                    Role.CUSTOMER);
        };
    }

    private void ensure(AppUserRepository users, PasswordEncoder encoder,
                        String email, String rawPassword, String name, Role... roles) {
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        users.save(AppUser.create(email, encoder.encode(rawPassword), name, roles));
        log.info("Seeded account {}", email);
    }
}
