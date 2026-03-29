package com.owaspdemo.a01_broken_access_control;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.repository.UserRepository;
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
@RequestMapping("/api/v1/vulnerable/accounts")
@Tag(name = "A01 - Broken Access Control", description = "IDOR — no auth, no ownership check")
public class VulnerableAccountController {

    private final UserRepository userRepository;

    public VulnerableAccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get any user profile (no auth)", description = "IDOR: change userId to access any account, SSN included")
    public ResponseEntity<?> getAccount(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId) {
        // BAD: No authentication, no authorization check — classic IDOR
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update any user's email (no auth)")
    public ResponseEntity<?> updateEmail(
            @Parameter(description = "User ID", example = "2") @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"email\": \"hacked@evil.com\"}")))
            @RequestBody Map<String, String> body) {
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
