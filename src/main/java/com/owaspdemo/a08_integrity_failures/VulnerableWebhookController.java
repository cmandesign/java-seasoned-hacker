package com.owaspdemo.a08_integrity_failures;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vulnerable/webhook")
@Tag(name = "A08 - Integrity Failures")
public class VulnerableWebhookController {

    @PostMapping
    @Operation(summary = "Receive webhook (no signature check)", description = "Anyone can send a fake webhook — accepted blindly")
    public Map<String, Object> handleWebhook(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"event\": \"payment.completed\", \"orderId\": \"12345\"}")))
            @RequestBody Map<String, Object> payload) {
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
