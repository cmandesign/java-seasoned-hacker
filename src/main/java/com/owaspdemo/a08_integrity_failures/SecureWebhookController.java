package com.owaspdemo.a08_integrity_failures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A08:2025 - Software and Data Integrity Failures
 *
 * SECURE: Verifies HMAC-SHA256 signature in X-Webhook-Signature header
 * before processing the webhook payload.
 *
 * Try: POST /api/v1/secure/webhook without valid signature -> 401
 * Try: POST /api/v1/secure/webhook with tampered body -> 401
 *
 * The shared secret is "webhook-secret-key" for demo purposes.
 */
@RestController
@RequestMapping("/api/v1/secure/webhook")
public class SecureWebhookController {

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier("webhook-secret-key");
    private final ObjectMapper objectMapper;

    public SecureWebhookController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {

        // GOOD: Reject if no signature provided
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing X-Webhook-Signature header"));
        }

        // GOOD: Verify signature against raw body before parsing
        if (!verifier.verify(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid webhook signature — payload may be tampered"));
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            return ResponseEntity.ok(Map.of(
                    "processed", true,
                    "event", payload.get("event"),
                    "signatureVerified", true
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid JSON payload"));
        }
    }

    /** Helper endpoint to generate a valid signature for testing */
    @PostMapping("/sign")
    public Map<String, String> sign(@RequestBody String payload) {
        return Map.of("signature", verifier.computeSignature(payload));
    }
}
