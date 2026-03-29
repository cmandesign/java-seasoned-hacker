package com.owaspdemo.a03_xss;

import com.owaspdemo.common.model.Feedback;
import com.owaspdemo.common.repository.FeedbackRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A03:2025 - Cross-Site Scripting (XSS)
 *
 * SECURE feedback controller:
 * 1. Sanitizes user input by stripping HTML tags before storage
 * 2. Requires authentication to submit feedback
 * 3. Uses the authenticated username (not user-supplied)
 * 4. Input length validation
 */
@RestController
@Tag(name = "A03 - XSS", description = "Stored XSS via unsanitized feedback vs sanitized output")
public class SecureFeedbackController {

    private final FeedbackRepository feedbackRepository;

    public SecureFeedbackController(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @PostMapping("/api/v1/secure/feedback")
    @Operation(summary = "Submit feedback (sanitized)",
            description = "Stores feedback after stripping HTML tags. Uses authenticated username.")
    public ResponseEntity<?> submitFeedback(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            value = "{\"message\": \"Great app! Very useful.\"}")))
            @RequestBody Map<String, String> body) {

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        if (message.length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message too long (max 2000 characters)"));
        }

        // GOOD: Use authenticated username, not user-supplied
        String username = authentication.getName();

        // GOOD: Strip all HTML tags to prevent stored XSS
        String sanitizedMessage = message.replaceAll("<[^>]*>", "");

        Feedback feedback = new Feedback(username, sanitizedMessage);
        feedbackRepository.save(feedback);

        return ResponseEntity.ok(Map.of(
                "status", "Feedback submitted",
                "note", "HTML tags have been stripped from your message."
        ));
    }

    @GetMapping("/api/v1/secure/feedback")
    @Operation(summary = "View all feedback (sanitized output)",
            description = "Returns all feedback. Content was sanitized on input.")
    public List<Feedback> getAllFeedback() {
        // GOOD: Content was sanitized before storage
        return feedbackRepository.findAllByOrderBySubmittedAtDesc();
    }
}
