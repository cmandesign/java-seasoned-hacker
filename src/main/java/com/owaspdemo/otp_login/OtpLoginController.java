package com.owaspdemo.otp_login;

import com.owaspdemo.a07_authentication_failures.SecureJwtUtil;
import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.repository.UserRepository;
import com.owaspdemo.config.RedisLoginCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure/otp-login")
@Tag(name = "OTP Login", description = "OTP-based passwordless login via SMS. OTP stored in Redis with 5-min TTL.")
public class OtpLoginController {

    private final UserRepository userRepository;
    private final RedisOtpService redisOtpService;
    private final FakeSmsService fakeSmsService;
    private final SecureJwtUtil secureJwtUtil;
    private final RedisLoginCacheService redisLoginCacheService;

    public OtpLoginController(UserRepository userRepository,
                              RedisOtpService redisOtpService,
                              FakeSmsService fakeSmsService,
                              SecureJwtUtil secureJwtUtil,
                              RedisLoginCacheService redisLoginCacheService) {
        this.userRepository = userRepository;
        this.redisOtpService = redisOtpService;
        this.fakeSmsService = fakeSmsService;
        this.secureJwtUtil = secureJwtUtil;
        this.redisLoginCacheService = redisLoginCacheService;
    }

    @PostMapping("/request")
    @Operation(summary = "Request OTP for login",
            description = "Accepts a username, generates a 6-digit OTP stored in Redis (5-min TTL), and sends a fake SMS to the user's phone number.")
    public ResponseEntity<Map<String, String>> requestOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"alice\"}")))
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username is required"));
        }

        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            // Don't reveal whether the user exists — return same success message
            return ResponseEntity.ok(Map.of(
                    "message", "If the account exists, an OTP has been sent to the registered phone number."
            ));
        }

        AppUser user = userOpt.get();
        String otp = redisOtpService.generateAndStore(username);
        fakeSmsService.sendOtp(user.getPhoneNumber(), username, otp);

        return ResponseEntity.ok(Map.of(
                "message", "If the account exists, an OTP has been sent to the registered phone number."
        ));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP and get JWT token",
            description = "Verifies the OTP from Redis. On success, returns a JWT token for authenticated access.")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"alice\", \"otp\": \"123456\"}")))
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        String otp = body.get("otp");

        if (username == null || username.isBlank() || otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username and otp are required"));
        }

        boolean valid = redisOtpService.verify(username, otp);
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired OTP"));
        }

        AppUser user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired OTP"));
        }

        String token = secureJwtUtil.generateToken(
                username, user.getRole().name(), user.getId(),
                user.getFirstName(), user.getLastName(), user.getPhoneNumber());
        redisLoginCacheService.cacheLoginToken(username, token);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "message", "OTP verified successfully. Use this token for authenticated requests."
        ));
    }
}
