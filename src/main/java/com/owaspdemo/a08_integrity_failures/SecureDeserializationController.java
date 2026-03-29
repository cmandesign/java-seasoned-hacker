package com.owaspdemo.a08_integrity_failures;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure")
@Tag(name = "A08 - Integrity Failures", description = "JSON DTOs + HMAC webhook signature verification")
public class SecureDeserializationController {

    private final ObjectMapper objectMapper;

    public SecureDeserializationController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ImportItem(String name, String category, double price) {}

    @PostMapping("/import")
    @Operation(summary = "Import via JSON DTOs (no ObjectInputStream)", description = "Strongly-typed records — no gadget chains possible")
    public Map<String, Object> importData(@RequestBody List<ImportItem> items) {
        // GOOD: Strongly-typed DTO — Jackson only maps to known fields
        // No arbitrary class instantiation, no gadget chains possible
        return Map.of(
                "imported", true,
                "count", items.size(),
                "items", items
        );
    }
}
