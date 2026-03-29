package com.owaspdemo.a08_integrity_failures;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure/webhook")
@Tag(name = "A08 - Integrity Failures")
public class SecureWebhookController {

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier("webhook-secret-key");
    private final ObjectMapper objectMapper;

    public SecureWebhookController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Receive webhook (HMAC-SHA256 verified)", description = "Requires X-Webhook-Signature header. Use /sign endpoint to generate.")
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawBody,
            @Parameter(description = "HMAC-SHA256 signature (sha256=hex)", example = "sha256=abc123")
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

    @PostMapping("/sign")
    @Operation(summary = "Generate HMAC signature for testing", description = "Returns sha256=hex signature for the given payload")
    public Map<String, String> sign(@RequestBody String payload) {
        return Map.of("signature", verifier.computeSignature(payload));
    }
}
