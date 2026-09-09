package com.example.storefront.security;

/** What {@code /api/auth/register} and {@code /api/auth/login} return. */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
