package com.owaspdemo.a08_integrity_failures;

import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Map;

/**
 * A08:2025 - Software and Data Integrity Failures
 *
 * VULNERABLE: Deserializes arbitrary Java objects from untrusted Base64 input.
 * An attacker can craft a serialized payload that executes code on readObject().
 *
 * Try: POST /api/v1/vulnerable/import with a Base64-encoded serialized object.
 * In a real attack, this would use gadget chains (ysoserial) for RCE.
 */
@RestController
@RequestMapping("/api/v1/vulnerable")
public class VulnerableDeserializationController {

    @PostMapping("/import")
    public Map<String, Object> importData(@RequestBody Map<String, String> body) throws Exception {
        String base64Data = body.get("data");
        byte[] bytes = Base64.getDecoder().decode(base64Data);

        // BAD: Deserializing untrusted data — classic RCE vector
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object obj = ois.readObject(); // Arbitrary code execution possible here
            return Map.of(
                    "imported", true,
                    "type", obj.getClass().getName(),
                    "data", obj.toString()
            );
        }
    }
}
