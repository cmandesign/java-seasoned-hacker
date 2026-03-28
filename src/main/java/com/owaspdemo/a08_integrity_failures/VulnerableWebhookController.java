package com.owaspdemo.a08_integrity_failures;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A08:2025 - Software and Data Integrity Failures
 *
 * VULNERABLE: Accepts webhook payloads without verifying the signature.
 * An attacker can forge a webhook to trigger fake payment confirmations.
 *
 * Try: POST /api/v1/vulnerable/webhook with any JSON body — accepted blindly.
 */
@RestController
@RequestMapping("/api/v1/vulnerable/webhook")
public class VulnerableWebhookController {

    @PostMapping
    public Map<String, Object> handleWebhook(@RequestBody Map<String, Object> payload) {
        // BAD: No signature verification — anyone can send a fake webhook
        String event = (String) payload.get("event");
        String orderId = (String) payload.get("orderId");

        // Process the webhook blindly
        return Map.of(
                "processed", true,
                "event", event,
                "orderId", orderId,
                "warning", "This webhook was accepted without any signature verification!"
        );
    }
}
