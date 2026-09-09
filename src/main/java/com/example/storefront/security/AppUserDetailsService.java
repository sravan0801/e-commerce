package com.example.storefront.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bridges the {@code app_user} table to Spring Security's password login flow. */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmailIgnoreCase(email)
                .map(AppUserPrincipal::fromEntity)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));
    }
}
