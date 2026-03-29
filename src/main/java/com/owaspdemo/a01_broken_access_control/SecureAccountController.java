package com.owaspdemo.a01_broken_access_control;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.model.Ticket;
import com.owaspdemo.common.repository.TicketRepository;
import com.owaspdemo.common.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/secure/accounts")
@Tag(name = "A01 - Broken Access Control", description = "Ownership check via @PreAuthorize, SSN hidden")
public class SecureAccountController {

    private static final String UPLOAD_DIR = "uploads/photos";

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public SecureAccountController(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
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

    @PostMapping("/{userId}/photo")
    @PreAuthorize("@accountOwnership.isOwner(authentication, #userId)")
    @Operation(summary = "Upload profile photo (path traversal safe)",
            description = "GOOD: ignores user-supplied filename, generates a safe UUID-based name")
    public ResponseEntity<?> uploadPhoto(
            @Parameter(description = "User ID", example = "2") @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        // GOOD: Ignore the user-supplied filename entirely
        // Generate a random filename to prevent path traversal
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        // Only allow image extensions
        if (!List.of(".jpg", ".jpeg", ".png", ".gif").contains(extension.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
        }

        String safeFilename = UUID.randomUUID() + extension;
        Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        Path destination = uploadPath.resolve(safeFilename).normalize();

        // GOOD: Verify the resolved path is still within the upload directory
        if (!destination.startsWith(uploadPath)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file path"));
        }

        file.transferTo(destination);

        return ResponseEntity.ok(Map.of(
                "message", "Photo uploaded successfully",
                "filename", safeFilename
        ));
    }

    @GetMapping("/{userId}/tickets")
    @PreAuthorize("@accountOwnership.isOwner(authentication, #userId)")
    @Operation(summary = "Get user's tickets (ownership enforced)")
    public ResponseEntity<?> getTickets(
            @Parameter(description = "User ID", example = "2") @PathVariable Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        List<Map<String, Object>> result = tickets.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("eventName", t.getEventName());
            map.put("quantity", t.getQuantity());
            map.put("price", t.getPrice());
            map.put("ticketHolders", t.getTicketHolders());
            map.put("purchasedAt", t.getPurchasedAt());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
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
