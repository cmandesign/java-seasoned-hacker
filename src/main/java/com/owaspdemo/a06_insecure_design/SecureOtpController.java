package com.owaspdemo.a06_insecure_design;

import com.owaspdemo.a07_authentication_failures.SecureJwtUtil;
import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.repository.UserRepository;
import com.owaspdemo.config.RedisLoginCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure/otp")
@Tag(name = "A06 - Insecure Design")
public class SecureOtpController {

    private final OtpService otpService;
    private final RateLimiter rateLimiter;
    private final UserRepository userRepository;
    private final SecureJwtUtil secureJwtUtil;
    private final RedisLoginCacheService redisLoginCacheService;

    public SecureOtpController(OtpService otpService, RateLimiter rateLimiter,
                               UserRepository userRepository, SecureJwtUtil secureJwtUtil,
                               RedisLoginCacheService redisLoginCacheService) {
        this.otpService = otpService;
        this.rateLimiter = rateLimiter;
        this.userRepository = userRepository;
        this.secureJwtUtil = secureJwtUtil;
        this.redisLoginCacheService = redisLoginCacheService;
    }

    @PostMapping("/generate")
    @Operation(
            summary = "Generate 6-digit OTP (5-min expiry, max 5 attempts)",
            description = "Accepts a username and generates a secure 6-digit OTP with 5-minute expiry and attempt lockout.",
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
        String otp = otpService.generateStrong(username);
        return ResponseEntity.ok(Map.of(
                "username", username,
                "message", "OTP sent via SMS (for demo: OTP is " + otp + ")",
                "note", "6 digits, 5-min expiry, max 5 attempts before lockout"
        ));
    }

    @PostMapping("/verify")
    @Operation(
            summary = "Verify OTP (max 5 attempts then 429 lockout)",
            description = "Verifies the 6-digit OTP for the given username. Locks out after 5 failed attempts."
    )
    public ResponseEntity<Map<String, Object>> verify(
            @Parameter(description = "Username the OTP was generated for", example = "alice") @RequestParam String username,
            @Parameter(description = "6-digit OTP guess", example = "000000") @RequestParam String otp) {
        // GOOD: Check rate limit before processing
        if (rateLimiter.isBlocked(username)) {
            otpService.invalidate(username);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                            "valid", false,
                            "message", "Too many attempts. Session locked out. Request a new OTP."
                    ));
        }

        boolean valid = otpService.verifyStrong(username, otp);

        if (!valid) {
            rateLimiter.recordAttempt(username);
            int remaining = 5 - rateLimiter.getAttempts(username);
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "attemptsRemaining", Math.max(remaining, 0)
            ));
        }

        rateLimiter.reset(username);
        otpService.invalidate(username);

        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "User not found"));
        }

        String token = secureJwtUtil.generateToken(
                username, user.getRole().name(), user.getId(),
                user.getFirstName(), user.getLastName(), user.getPhoneNumber());
        redisLoginCacheService.cacheLoginToken(username, token);

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "token", token,
                "message", "OTP verified successfully. Use this token for authenticated requests."));
    }
}
