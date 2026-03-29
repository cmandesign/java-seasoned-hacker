package com.owaspdemo.a02_security_misconfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vulnerable/debug")
@Tag(name = "A02 - Security Misconfiguration", description = "Stack traces, actuator secrets, H2 console exposure. Run with --spring.profiles.active=vulnerable")
public class MisconfigDemoController {

    @GetMapping("/error")
    @Operation(summary = "Trigger exception (leak stack trace with vulnerable profile)")
    public String triggerError() {
        // Intentional error to show how misconfiguration leaks internal details
        throw new RuntimeException("Database connection failed: jdbc:postgresql://prod-db.internal:5432/customers password=Sup3rS3cret!");
    }

    @GetMapping("/config")
    @Operation(summary = "Check if secrets are exposed via actuator")
    public String showConfig() {
        // In a misconfigured app, this might expose environment variables
        return "Visit /actuator/env to see if secrets are exposed (requires 'vulnerable' profile)";
    }
}
