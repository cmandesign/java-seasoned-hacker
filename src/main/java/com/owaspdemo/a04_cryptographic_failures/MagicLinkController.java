package com.owaspdemo.a04_cryptographic_failures;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Tag(name = "A04 - Cryptographic Failures", description = "Crackable magic link JWT vs strong 256-bit key with expiry")
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
    @Operation(summary = "Generate crackable magic link", description = "JWT signed with weak secret 'password123'. Crack with: hashcat -m 16500")
    public Map<String, String> generateVulnerable(
            @Parameter(description = "User email", example = "alice@company.com") @RequestParam String email) {
        String token = vulnerableService.generateToken(email);
        return Map.of(
                "token", token,
                "hint", "This token is Base64(JWT signed with 'password123'). Try cracking it with hashcat!"
        );
    }

    @GetMapping("/api/v1/vulnerable/magic-link/verify")
    @Operation(summary = "Verify magic link (replayable, no expiry)")
    public Map<String, Object> verifyVulnerable(
            @Parameter(description = "Base64-encoded JWT token from generate endpoint") @RequestParam String token) {
        return vulnerableService.verifyToken(token);
    }

    // --- Secure endpoints ---

    @PostMapping("/api/v1/secure/magic-link")
    @Operation(summary = "Generate secure magic link", description = "256-bit random key, 5-min expiry, single-use")
    public Map<String, String> generateSecure(
            @Parameter(description = "User email", example = "alice@company.com") @RequestParam String email) {
        String token = secureService.generateToken(email);
        return Map.of(
                "token", token,
                "note", "Signed with 256-bit random key. Expires in 5 minutes. Single-use."
        );
    }

    @GetMapping("/api/v1/secure/magic-link/verify")
    @Operation(summary = "Verify magic link (single-use, 5-min expiry)")
    public Map<String, Object> verifySecure(
            @Parameter(description = "JWT token from generate endpoint") @RequestParam String token) {
        return secureService.verifyToken(token);
    }
}
