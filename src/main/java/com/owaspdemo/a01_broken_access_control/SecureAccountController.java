package com.owaspdemo.a01_broken_access_control;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure/accounts")
@Tag(name = "A01 - Broken Access Control", description = "Ownership check via @PreAuthorize, SSN hidden")
public class SecureAccountController {

    private final UserRepository userRepository;

    public SecureAccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("@accountOwnership.isOwner(authentication, #userId)")
    @Operation(summary = "Get user profile (ownership enforced)", description = "Auth: JWT. alice can read id=2, admin can read any.")
    public ResponseEntity<?> getAccount(
            @Parameter(description = "User ID", example = "2") @PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(toSafeDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@accountOwnership.isOwner(authentication, #userId)")
    @Operation(summary = "Update own email (ownership enforced)")
    public ResponseEntity<?> updateEmail(
            @Parameter(description = "User ID", example = "2") @PathVariable Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"email\": \"new@company.com\"}")))
            @RequestBody Map<String, String> body) {
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
