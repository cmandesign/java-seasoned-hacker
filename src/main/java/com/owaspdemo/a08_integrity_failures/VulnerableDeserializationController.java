package com.owaspdemo.a08_integrity_failures;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vulnerable")
@Tag(name = "A08 - Integrity Failures", description = "Unsafe deserialization + unverified webhooks")
public class VulnerableDeserializationController {

    @PostMapping("/import")
    @Operation(summary = "Import via Java deserialization (RCE vector)", description = "Accepts Base64-encoded serialized Java object. Use ysoserial for real exploits.")
    public Map<String, Object> importData(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"data\": \"rO0ABXQADUhlbGxvIFdvcmxkIQ==\"}")))
            @RequestBody Map<String, String> body) throws Exception {
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
