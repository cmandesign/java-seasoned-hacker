package com.owaspdemo.a06_insecure_design;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vulnerable/otp")
@Tag(name = "A06 - Insecure Design", description = "4-digit OTP with no rate limit vs 6-digit with lockout")
public class VulnerableOtpController {

    private final OtpService otpService;

    public VulnerableOtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate 4-digit OTP (no rate limit, no expiry)")
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
    @Operation(summary = "Verify OTP (unlimited attempts)")
    public Map<String, Object> verify(
            @Parameter(description = "Session ID from generate", example = "550e8400-e29b-41d4-a716-446655440000") @RequestParam String sessionId,
            @Parameter(description = "4-digit OTP guess", example = "0000") @RequestParam String otp) {
        // BAD: No rate limiting, no attempt counter, no lockout
        boolean valid = otpService.verifyWeak(sessionId, otp);
        return Map.of("valid", valid);
    }
}
