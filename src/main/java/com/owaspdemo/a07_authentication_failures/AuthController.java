package com.owaspdemo.a07_authentication_failures;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A07:2025 - Authentication Failures
 *
 * Vulnerable flow:
 *   1. POST /api/v1/vulnerable/auth/login  -> get JWT with weak secret
 *   2. GET  /api/v1/vulnerable/auth/me?token=...  -> access protected resource
 *   3. Attacker cracks the secret, forges a token with role=ADMIN
 *
 * Secure flow:
 *   1. POST /api/v1/secure/auth/login  -> get RSA-signed JWT (15-min expiry)
 *   2. GET  /api/v1/secure/auth/me?token=...  -> validates alg, issuer, audience, expiry
 *   3. Forged/tampered tokens are rejected
 */
@RestController
public class AuthController {

    private final VulnerableJwtUtil vulnerableJwt;
    private final SecureJwtUtil secureJwt;

    public AuthController(VulnerableJwtUtil vulnerableJwt, SecureJwtUtil secureJwt) {
        this.vulnerableJwt = vulnerableJwt;
        this.secureJwt = secureJwt;
    }

    // --- Vulnerable ---

    @PostMapping("/api/v1/vulnerable/auth/login")
    public Map<String, String> vulnerableLogin(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "guest");
        String role = body.getOrDefault("role", "USER");
        String token = vulnerableJwt.generateToken(username, role);
        return Map.of(
                "token", token,
                "hint", "Secret is 'secretsecretsecretsecretsecretsecret'. " +
                        "Try decoding at jwt.io and forging a token with role=ADMIN."
        );
    }

    @GetMapping("/api/v1/vulnerable/auth/me")
    public Map<String, Object> vulnerableMe(@RequestParam String token) {
        return vulnerableJwt.parseToken(token);
    }

    // --- Secure ---

    @PostMapping("/api/v1/secure/auth/login")
    public Map<String, String> secureLogin(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "guest");
        String role = body.getOrDefault("role", "USER");
        String token = secureJwt.generateToken(username, role);
        return Map.of(
                "token", token,
                "note", "RSA-256 signed. 15-min expiry. Tamper and it will be rejected."
        );
    }

    @GetMapping("/api/v1/secure/auth/me")
    public Map<String, Object> secureMe(@RequestParam String token) {
        return secureJwt.parseToken(token);
    }
}
