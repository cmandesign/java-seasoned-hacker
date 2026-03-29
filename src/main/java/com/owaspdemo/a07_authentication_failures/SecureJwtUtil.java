package com.owaspdemo.a07_authentication_failures;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * A07:2025 - Authentication Failures
 *
 * SECURE JWT utility:
 * 1. Uses RSA-2048 key pair (asymmetric) — private key signs, public key verifies
 * 2. 15-minute expiration
 * 3. Explicit algorithm validation (RS256 only)
 * 4. Issuer and audience claims validated
 */
@Component
public class SecureJwtUtil {

    private static final String ISSUER = "owasp-demo";
    private static final String AUDIENCE = "owasp-demo-app";

    // GOOD: RSA-2048 key pair generated at startup
    private final KeyPair keyPair = Jwts.SIG.RS256.keyPair().build();

    public String generateToken(String username, String role, Long userId) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900))) // 15 minutes
                .signWith(keyPair.getPrivate()) // GOOD: signed with private key
                .compact();
    }

    public Map<String, Object> parseToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(keyPair.getPublic()) // GOOD: verify with public key only
                    .requireIssuer(ISSUER)
                    .requireAudience(AUDIENCE)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Map.of(
                    "username", claims.getSubject(),
                    "role", claims.get("role", String.class),
                    "userId", claims.get("userId", Long.class),
                    "expires", claims.getExpiration().toString()
            );
        } catch (JwtException e) {
            return Map.of("error", "Authentication failed: " + e.getMessage());
        }
    }
}
