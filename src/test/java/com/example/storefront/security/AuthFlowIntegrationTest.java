package com.example.storefront.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.storefront.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Register -> login -> use the access token on a protected endpoint. */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;

    @Test
    void registerThenAccessProtectedResource() {
        ResponseEntity<AuthResponse> registered = rest.postForEntity("/api/auth/register",
                json("""
                     {"email":"flow@test.local","password":"password123","fullName":"Flow Tester"}
                     """), AuthResponse.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accessToken = registered.getBody().accessToken();
        assertThat(accessToken).isNotBlank();

        // No token -> 401
        ResponseEntity<String> anonymous = rest.getForEntity("/api/orders", String.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // With token -> 200
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<String> authed = rest.exchange("/api/orders", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(authed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
