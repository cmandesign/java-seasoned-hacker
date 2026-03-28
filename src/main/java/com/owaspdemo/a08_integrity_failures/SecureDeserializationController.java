package com.owaspdemo.a08_integrity_failures;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * A08:2025 - Software and Data Integrity Failures
 *
 * SECURE: Uses JSON with concrete DTOs instead of Java serialization.
 * No polymorphic deserialization, no ObjectInputStream.
 */
@RestController
@RequestMapping("/api/v1/secure")
public class SecureDeserializationController {

    private final ObjectMapper objectMapper;

    public SecureDeserializationController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ImportItem(String name, String category, double price) {}

    @PostMapping("/import")
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
