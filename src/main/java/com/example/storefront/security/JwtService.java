package com.example.storefront.security;

import com.example.storefront.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies short-lived HS256 access tokens. Refresh tokens are opaque
 * and handled by {@link AuthService} against the {@code refresh_token} table.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(AppProperties props) {
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = props.jwt().issuer();
        this.accessTtl = props.jwt().accessTokenTtl();
    }

    public Duration accessTokenTtl() {
        return accessTtl;
    }

    public String issueAccessToken(AppUser user) {
        Instant now = Instant.now();
        List<String> authorities = user.getRoles().stream().map(Role::authority).toList();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("authorities", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    /** @throws JwtException if the token is missing, malformed, expired or forged. */
    public ParsedToken parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
        Claims c = jws.getPayload();
        Long uid = c.get("uid", Number.class).longValue();
        @SuppressWarnings("unchecked")
        List<String> authorities = c.get("authorities", List.class);
        return new ParsedToken(uid, c.getSubject(), authorities == null ? List.of() : authorities);
    }

    public record ParsedToken(Long userId, String email, List<String> authorities) {
    }
}
