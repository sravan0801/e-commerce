package com.example.storefront.security;

import com.example.storefront.common.DomainExceptions.BusinessRuleException;
import com.example.storefront.common.DomainExceptions.NotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and password login. Both return a short-lived JWT access token;
 * there is no refresh token -- when it expires, the client logs in again.
 */
@Service
public class AuthService {

    private final AppUserRepository users;
    private final AuthenticationManager authManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository users,
                       AuthenticationManager authManager,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.users = users;
        this.authManager = authManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(String email, String rawPassword, String fullName) {
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An account with that email already exists.");
        }
        AppUser user = AppUser.create(email.toLowerCase(), passwordEncoder.encode(rawPassword),
                fullName, Role.CUSTOMER);
        users.save(user);
        return token(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String rawPassword) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, rawPassword));
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        AppUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Account not found."));
        return token(user);
    }

    private AuthResponse token(AppUser user) {
        return AuthResponse.bearer(
                jwtService.issueAccessToken(user),
                jwtService.accessTokenTtl().toSeconds());
    }
}
