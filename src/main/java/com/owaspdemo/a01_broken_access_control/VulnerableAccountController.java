package com.owaspdemo.a01_broken_access_control;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.model.Ticket;
import com.owaspdemo.common.repository.TicketRepository;
import com.owaspdemo.common.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vulnerable/accounts")
@Tag(name = "A01 - Broken Access Control", description = "IDOR — no auth, no ownership check")
public class VulnerableAccountController {

    private static final String UPLOAD_DIR = "uploads/photos";

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public VulnerableAccountController(UserRepository userRepository, TicketRepository ticketRepository) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
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

    @PostMapping("/{userId}/photo")
    @Operation(summary = "Upload profile photo (path traversal vulnerable)",
            description = "BAD: uses original filename directly — attacker can send ../../etc/cron.d/evil")
    public ResponseEntity<?> uploadPhoto(
            @Parameter(description = "User ID", example = "2") @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) throws IOException {
        // BAD: Using the client-supplied filename without any sanitization
        // An attacker can upload a file named "../../etc/cron.d/backdoor" to write anywhere on disk
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Filename is required"));
        }
        Path uploadPath = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadPath);

        // BAD: directly concatenating user-controlled filename — classic path traversal
        Path destination = uploadPath.resolve(originalFilename);
        file.transferTo(destination.toFile());

        return ResponseEntity.ok(Map.of(
                "message", "Photo uploaded successfully",
                "path", destination.toString()
        ));
    }

    @GetMapping("/{userId}/tickets")
    @Operation(summary = "Get any user's tickets (no auth)", description = "IDOR: no ownership check on tickets")
    public ResponseEntity<?> getTickets(
            @Parameter(description = "User ID", example = "2") @PathVariable Long userId) {
        // BAD: No authentication, no authorization check
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
