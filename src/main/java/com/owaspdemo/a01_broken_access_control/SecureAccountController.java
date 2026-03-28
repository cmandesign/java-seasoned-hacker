package com.owaspdemo.a01_broken_access_control;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A01:2025 - Broken Access Control
 *
 * SECURE: Uses @PreAuthorize with a custom ownership check.
 * Regular users can only access their own profile. Admins can access any.
 * SSN is never exposed in the response.
 *
 * Try as alice: GET /api/v1/secure/accounts/2  -> 200 (her own)
 * Try as alice: GET /api/v1/secure/accounts/1  -> 403 (admin's profile)
 * Try as admin: GET /api/v1/secure/accounts/2  -> 200 (admin can access any)
 *
 * Auth: HTTP Basic (alice:Alice123!)
 */
@RestController
@RequestMapping("/api/v1/secure/accounts")
public class SecureAccountController {

    private final UserRepository userRepository;

    public SecureAccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@accountOwnership.isOwner(authentication, #userId)")
    public ResponseEntity<?> getAccount(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(toSafeDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@accountOwnership.isOwner(authentication, #userId)")
    public ResponseEntity<?> updateEmail(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setEmail(body.get("email"));
                    userRepository.save(user);
                    return ResponseEntity.ok(toSafeDto(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toSafeDto(AppUser user) {
        // GOOD: SSN and password hash are never exposed
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole()
        );
    }
}
