package com.owaspdemo.a02_security_misconfiguration;

import org.springframework.web.bind.annotation.*;

/**
 * A02:2025 - Security Misconfiguration
 *
 * This endpoint intentionally throws an exception to demonstrate
 * how error handling differs between vulnerable and secure profiles.
 *
 * With --spring.profiles.active=vulnerable:
 *   GET /api/v1/vulnerable/debug/error  -> Full stack trace, exception class, internal paths
 *
 * Without (secure default):
 *   GET /api/v1/vulnerable/debug/error  -> Generic "An unexpected error occurred"
 */
@RestController
@RequestMapping("/api/v1/vulnerable/debug")
public class MisconfigDemoController {

    @GetMapping("/error")
    public String triggerError() {
        // Intentional error to show how misconfiguration leaks internal details
        throw new RuntimeException("Database connection failed: jdbc:postgresql://prod-db.internal:5432/customers password=Sup3rS3cret!");
    }

    @GetMapping("/config")
    public String showConfig() {
        // In a misconfigured app, this might expose environment variables
        return "Visit /actuator/env to see if secrets are exposed (requires 'vulnerable' profile)";
    }
}
