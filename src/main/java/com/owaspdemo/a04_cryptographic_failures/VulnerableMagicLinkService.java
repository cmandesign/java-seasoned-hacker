package com.owaspdemo.a04_cryptographic_failures;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

/**
 * A04:2025 - Cryptographic Failures
 *
 * VULNERABLE: Generates magic link tokens with a weak, crackable HMAC secret.
 *
 * Problems:
 * 1. Secret "password123" can be cracked offline (hashcat/john)
 * 2. No token expiry — valid forever
 * 3. No single-use enforcement — replay attacks possible
 * 4. Token contains email in plain text (Base64 is NOT encryption)
 */
@Service
public class VulnerableMagicLinkService {

    // BAD: Weak secret that can be brute-forced offline
    // Padded to 32 bytes for JJWT minimum, but still trivially crackable
    private static final SecretKey WEAK_KEY = Keys.hmacShaKeyFor(
            "password123-----padding---------".getBytes(StandardCharsets.UTF_8));

    public String generateToken(String email) {
        String jwt = Jwts.builder()
                .subject(email)
                .claim("type", "magic-link")
                // BAD: No expiration set
                .signWith(WEAK_KEY)
                .compact();

        // Return as Base64 (looks encrypted to the untrained eye, but isn't)
        return Base64.getUrlEncoder().encodeToString(jwt.getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, Object> verifyToken(String token) {
        String jwt = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);

        var claims = Jwts.parser()
                .verifyWith(WEAK_KEY)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();

        // BAD: No single-use check, no expiry validation
        return Map.of(
                "email", claims.getSubject(),
                "valid", true,
                "message", "Magic link verified — user authenticated"
        );
    }
}
