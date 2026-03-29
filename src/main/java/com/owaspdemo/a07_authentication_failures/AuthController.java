package com.owaspdemo.a07_authentication_failures;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Tag(name = "A07 - Authentication Failures", description = "Weak HMAC JWT vs RSA-2048 with issuer/audience/expiry")
public class AuthController {

    private final VulnerableJwtUtil vulnerableJwt;
    private final SecureJwtUtil secureJwt;

    public AuthController(VulnerableJwtUtil vulnerableJwt, SecureJwtUtil secureJwt) {
        this.vulnerableJwt = vulnerableJwt;
        this.secureJwt = secureJwt;
    }

    // --- Vulnerable ---

    @PostMapping("/api/v1/vulnerable/auth/login")
    @Operation(summary = "Login (weak HMAC JWT)", description = "Secret is 'secretsecretsecretsecretsecretsecret'. Forge a token with role=ADMIN at jwt.io")
    public Map<String, String> vulnerableLogin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"alice\", \"role\": \"USER\"}")))
            @RequestBody Map<String, String> body) {
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
    @Operation(summary = "Get current user from JWT (no expiry, no issuer check)")
    public Map<String, Object> vulnerableMe(
            @Parameter(description = "JWT token from login") @RequestParam String token) {
        return vulnerableJwt.parseToken(token);
    }

    // --- Secure ---

    @PostMapping("/api/v1/secure/auth/login")
    @Operation(summary = "Login (RSA-256 JWT)", description = "15-min expiry, validates issuer + audience")
    public Map<String, String> secureLogin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"alice\", \"role\": \"USER\"}")))
            @RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "guest");
        String role = body.getOrDefault("role", "USER");
        String token = secureJwt.generateToken(username, role);
        return Map.of(
                "token", token,
                "note", "RSA-256 signed. 15-min expiry. Tamper and it will be rejected."
        );
    }

    @GetMapping("/api/v1/secure/auth/me")
    @Operation(summary = "Get current user from JWT (validates alg, issuer, audience, expiry)")
    public Map<String, Object> secureMe(
            @Parameter(description = "RSA-signed JWT from login") @RequestParam String token) {
        return secureJwt.parseToken(token);
    }
}
