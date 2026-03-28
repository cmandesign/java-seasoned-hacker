package com.owaspdemo.a07_authentication_failures;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A07:2025 - Authentication Failures
 *
 * VULNERABLE JWT utility:
 * 1. Uses a trivially short HMAC secret ("secret") — brute-forceable
 * 2. No token expiration
 * 3. Does not validate algorithm — vulnerable to alg:none attack
 * 4. No issuer/audience validation
 */
@Component
public class VulnerableJwtUtil {

    // BAD: trivially weak secret
    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            "secretsecretsecretsecretsecretsecret".getBytes(StandardCharsets.UTF_8));

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                // BAD: no expiration
                .signWith(KEY)
                .compact();
    }

    public Map<String, Object> parseToken(String token) {
        // BAD: Uses the same weak key, no algorithm restriction
        var claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Map.of(
                "username", claims.getSubject(),
                "role", claims.get("role", String.class)
        );
    }
}
