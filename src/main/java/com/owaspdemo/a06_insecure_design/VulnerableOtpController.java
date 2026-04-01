package com.owaspdemo.a06_insecure_design;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vulnerable/otp")
@Tag(name = "A06 - Insecure Design", description = "4-digit OTP with no rate limit vs 6-digit with lockout")
public class VulnerableOtpController {

    private final OtpService otpService;

    public VulnerableOtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Generate 4-digit OTP (no rate limit, no expiry)",
            description = "Accepts a username and generates a weak 4-digit OTP with no expiry and no rate limiting.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"username\": \"alice\"}"),
                            examples = @ExampleObject(value = "{\"username\": \"alice\"}")
                    )
            )
    )
    public ResponseEntity<Map<String, String>> generate(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "username is required"));
        }
        String otp = otpService.generateWeak(username);
        return ResponseEntity.ok(Map.of(
                "username", username,
                "message", "OTP sent via SMS (for demo: OTP is " + otp + ")",
                "hint", "Only 4 digits, no rate limit. Try brute-forcing it!"
        ));
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify OTP (unlimited attempts)",
            description = "Verifies a 4-digit OTP for the given username with no rate limiting — vulnerable to brute-force."
    )
    public Map<String, Object> verify(
            @Parameter(description = "Username the OTP was generated for", example = "alice") @RequestParam String username,
            @Parameter(description = "4-digit OTP guess", example = "0000") @RequestParam String otp) {
        // BAD: No rate limiting, no attempt counter, no lockout
        boolean valid = otpService.verifyWeak(username, otp);
        return Map.of("valid", valid);
    }
}
