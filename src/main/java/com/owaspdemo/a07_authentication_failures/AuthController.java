package com.owaspdemo.a07_authentication_failures;

import com.owaspdemo.common.repository.UserRepository;
import com.owaspdemo.config.RedisLoginCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Tag(name = "A07 - Authentication Failures", description = "Weak HMAC JWT vs RSA-2048 with issuer/audience/expiry")
public class AuthController {

    private final VulnerableJwtUtil vulnerableJwt;
    private final SecureJwtUtil secureJwt;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisLoginCacheService redisLoginCacheService;

    public AuthController(VulnerableJwtUtil vulnerableJwt, SecureJwtUtil secureJwt,
                          UserRepository userRepository, PasswordEncoder passwordEncoder,
                          RedisLoginCacheService redisLoginCacheService) {
        this.vulnerableJwt = vulnerableJwt;
        this.secureJwt = secureJwt;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisLoginCacheService = redisLoginCacheService;
    }

    // --- Vulnerable ---

    @PostMapping("/api/v1/vulnerable/auth/login")
    @Operation(summary = "Login (weak HMAC JWT)", description = "Verifies credentials, then issues a JWT signed with weak secret. Crack it at jwt.io.")
    public ResponseEntity<?> vulnerableLogin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"alice\", \"password\": \"Alice123!\"}")))
            @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        var user = userRepository.findByUsername(username);
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        var appUser = user.get();
        String role = appUser.getRole().name();
        Long userId = appUser.getId();
        String token = vulnerableJwt.generateToken(username, role, userId,
                appUser.getFirstName(), appUser.getLastName(), appUser.getPhoneNumber());
        redisLoginCacheService.cacheLoginToken(username, token);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "hint", "Secret is 'secretsecretsecretsecretsecretsecret'. " +
                        "Try decoding at jwt.io and forging a token with role=ADMIN."
        ));
    }

    @GetMapping("/api/v1/vulnerable/auth/me")
    @Operation(summary = "Get current user from JWT (no expiry, no issuer check)")
    public Map<String, Object> vulnerableMe(
            @Parameter(description = "JWT token from login") @RequestParam String token) {
        return vulnerableJwt.parseToken(token);
    }

    // --- Secure ---

    @PostMapping("/api/v1/secure/auth/login")
    @Operation(summary = "Login (RSA-256 JWT)", description = "Verifies credentials, then issues RSA-signed JWT. 15-min expiry, validates issuer + audience.")
    public ResponseEntity<?> secureLogin(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"alice\", \"password\": \"Alice123!\"}")))
            @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        var user = userRepository.findByUsername(username);
        if (user.isEmpty() || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        var appUser = user.get();
        String role = appUser.getRole().name();
        Long userId = appUser.getId();
        String token = secureJwt.generateToken(username, role, userId,
                appUser.getFirstName(), appUser.getLastName(), appUser.getPhoneNumber());
        redisLoginCacheService.cacheLoginToken(username, token);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "note", "RSA-256 signed. 15-min expiry. Tamper and it will be rejected."
        ));
    }

    @GetMapping("/api/v1/secure/auth/me")
    @Operation(summary = "Get current user from JWT (validates alg, issuer, audience, expiry)")
    public Map<String, Object> secureMe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Map.of("error", "Not authenticated");
        }
        
        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("UNKNOWN");
        
        return Map.of(
                "username", username,
                "role", role
        );
    }
}
