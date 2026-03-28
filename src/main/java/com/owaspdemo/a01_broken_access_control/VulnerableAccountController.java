package com.owaspdemo.a01_broken_access_control;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A01:2025 - Broken Access Control
 *
 * VULNERABLE: Any caller can access any user's profile by changing the ID.
 * No authentication required, no ownership check.
 *
 * Try: GET /api/v1/vulnerable/accounts/1  (admin's profile with SSN!)
 * Try: GET /api/v1/vulnerable/accounts/2  (alice's profile)
 */
@RestController
@RequestMapping("/api/v1/vulnerable/accounts")
public class VulnerableAccountController {

    private final UserRepository userRepository;

    public VulnerableAccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getAccount(@PathVariable Long userId) {
        // BAD: No authentication, no authorization check — classic IDOR
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateEmail(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        // BAD: Any user can change any other user's email
        return userRepository.findById(userId)
                .map(user -> {
                    user.setEmail(body.get("email"));
                    userRepository.save(user);
                    return ResponseEntity.ok(toDto(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toDto(AppUser user) {
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "ssn", user.getSsn(),  // BAD: Exposing sensitive data
                "role", user.getRole()
        );
    }
}
