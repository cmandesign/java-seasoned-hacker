package com.owaspdemo.a04_cryptographic_failures;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A04:2025 - Cryptographic Failures — Magic Link Demo
 *
 * Vulnerable flow:
 *   1. POST /api/v1/vulnerable/magic-link?email=alice@company.com  -> returns crackable token
 *   2. GET  /api/v1/vulnerable/magic-link/verify?token=...         -> authenticates
 *   3. Attacker cracks the JWT secret offline, forges tokens for any email
 *
 * Secure flow:
 *   1. POST /api/v1/secure/magic-link?email=alice@company.com      -> returns strong token
 *   2. GET  /api/v1/secure/magic-link/verify?token=...             -> authenticates (once only)
 *   3. Second use of same token -> rejected (single-use)
 *   4. After 5 min -> rejected (expired)
 */
@RestController
public class MagicLinkController {

    private final VulnerableMagicLinkService vulnerableService;
    private final SecureMagicLinkService secureService;

    public MagicLinkController(VulnerableMagicLinkService vulnerableService,
                               SecureMagicLinkService secureService) {
        this.vulnerableService = vulnerableService;
        this.secureService = secureService;
    }

    // --- Vulnerable endpoints ---

    @PostMapping("/api/v1/vulnerable/magic-link")
    public Map<String, String> generateVulnerable(@RequestParam String email) {
        String token = vulnerableService.generateToken(email);
        return Map.of(
                "token", token,
                "hint", "This token is Base64(JWT signed with 'password123'). Try cracking it with hashcat!"
        );
    }

    @GetMapping("/api/v1/vulnerable/magic-link/verify")
    public Map<String, Object> verifyVulnerable(@RequestParam String token) {
        return vulnerableService.verifyToken(token);
    }

    // --- Secure endpoints ---

    @PostMapping("/api/v1/secure/magic-link")
    public Map<String, String> generateSecure(@RequestParam String email) {
        String token = secureService.generateToken(email);
        return Map.of(
                "token", token,
                "note", "Signed with 256-bit random key. Expires in 5 minutes. Single-use."
        );
    }

    @GetMapping("/api/v1/secure/magic-link/verify")
    public Map<String, Object> verifySecure(@RequestParam String token) {
        return secureService.verifyToken(token);
    }
}
