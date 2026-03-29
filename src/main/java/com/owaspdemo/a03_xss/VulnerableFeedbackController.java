package com.owaspdemo.a03_xss;

import com.owaspdemo.common.model.Feedback;
import com.owaspdemo.common.repository.FeedbackRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A03:2025 - Cross-Site Scripting (XSS)
 *
 * VULNERABLE feedback controller:
 * 1. Stores user input without any sanitization
 * 2. Returns raw HTML/script content in feedback messages
 * 3. No authentication required to submit feedback
 * 4. No input validation or encoding
 *
 * Try submitting: <script>alert('XSS')</script>
 * or: <img src=x onerror="alert('XSS')">
 */
@RestController
@Tag(name = "A03 - XSS", description = "Stored XSS via unsanitized feedback vs sanitized output")
public class VulnerableFeedbackController {

    private final FeedbackRepository feedbackRepository;

    public VulnerableFeedbackController(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @PostMapping("/api/v1/vulnerable/feedback")
    @Operation(summary = "Submit feedback (no sanitization)",
            description = "Stores feedback without any input sanitization. Try injecting <script>alert('XSS')</script>")
    public ResponseEntity<?> submitFeedback(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            value = "{\"username\": \"alice\", \"message\": \"<img src=x onerror=alert('XSS')> Great app!\"}")))
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        String message = body.get("message");

        if (username == null || message == null || username.isBlank() || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username and message are required"));
        }

        // BAD: No sanitization — stores raw user input
        Feedback feedback = new Feedback(username, message);
        feedbackRepository.save(feedback);

        return ResponseEntity.ok(Map.of(
                "status", "Feedback submitted",
                "hint", "Try submitting <script>alert('XSS')</script> as the message and view it in the admin panel."
        ));
    }

    @GetMapping("/api/v1/vulnerable/feedback")
    @Operation(summary = "View all feedback (renders raw HTML)",
            description = "Returns all feedback without output encoding. XSS payloads will execute when rendered.")
    public List<Feedback> getAllFeedback() {
        // BAD: Returns raw content — XSS payloads will execute in the browser
        return feedbackRepository.findAllByOrderBySubmittedAtDesc();
    }
}
