package com.owaspdemo.a06_insecure_design;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * A06:2025 - Insecure Design
 *
 * VULNERABLE: 4-digit OTP with no rate limiting, no expiry, no lockout.
 * An attacker can brute-force all 10,000 combinations in seconds.
 *
 * Demo:
 *   1. POST /api/v1/vulnerable/otp/generate          -> returns sessionId (OTP sent "via SMS")
 *   2. POST /api/v1/vulnerable/otp/verify?sessionId=...&otp=0000  -> try all 0000-9999
 *   3. No lockout, no rate limit — eventually succeeds
 */
@RestController
@RequestMapping("/api/v1/vulnerable/otp")
public class VulnerableOtpController {

    private final OtpService otpService;

    public VulnerableOtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    public Map<String, String> generate() {
        String sessionId = UUID.randomUUID().toString();
        String otp = otpService.generateWeak(sessionId);
        return Map.of(
                "sessionId", sessionId,
                "message", "OTP sent via SMS (for demo: OTP is " + otp + ")",
                "hint", "Only 4 digits, no rate limit. Try brute-forcing it!"
        );
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestParam String sessionId, @RequestParam String otp) {
        // BAD: No rate limiting, no attempt counter, no lockout
        boolean valid = otpService.verifyWeak(sessionId, otp);
        return Map.of("valid", valid);
    }
}
