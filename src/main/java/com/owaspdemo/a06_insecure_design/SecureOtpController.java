package com.owaspdemo.a06_insecure_design;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secure/otp")
@Tag(name = "A06 - Insecure Design")
public class SecureOtpController {

    private final OtpService otpService;
    private final RateLimiter rateLimiter;

    public SecureOtpController(OtpService otpService, RateLimiter rateLimiter) {
        this.otpService = otpService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate 6-digit OTP (5-min expiry, max 5 attempts)")
    public Map<String, String> generate() {
        String sessionId = UUID.randomUUID().toString();
        String otp = otpService.generateStrong(sessionId);
        return Map.of(
                "sessionId", sessionId,
                "message", "OTP sent via SMS (for demo: OTP is " + otp + ")",
                "note", "6 digits, 5-min expiry, max 5 attempts before lockout"
        );
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP (max 5 attempts then 429 lockout)")
    public ResponseEntity<Map<String, Object>> verify(
            @Parameter(description = "Session ID from generate", example = "550e8400-e29b-41d4-a716-446655440000") @RequestParam String sessionId,
            @Parameter(description = "6-digit OTP guess", example = "000000") @RequestParam String otp) {
        // GOOD: Check rate limit before processing
        if (rateLimiter.isBlocked(sessionId)) {
            otpService.invalidate(sessionId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "valid", false,
                            "message", "Too many attempts. Session locked out. Request a new OTP."
                    ));
        }

        boolean valid = otpService.verifyStrong(sessionId, otp);

        if (!valid) {
            rateLimiter.recordAttempt(sessionId);
            int remaining = 5 - rateLimiter.getAttempts(sessionId);
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "attemptsRemaining", Math.max(remaining, 0)
            ));
        }

        rateLimiter.reset(sessionId);
        otpService.invalidate(sessionId);
        return ResponseEntity.ok(Map.of("valid", true, "message", "OTP verified successfully"));
    }
}
