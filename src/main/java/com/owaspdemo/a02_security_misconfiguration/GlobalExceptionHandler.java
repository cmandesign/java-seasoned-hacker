package com.owaspdemo.a02_security_misconfiguration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.Map;

/**
 * A02:2025 - Security Misconfiguration
 *
 * SECURE: Returns generic error messages without stack traces or internal details.
 * The vulnerable behavior is controlled by Spring profiles (application-vulnerable.yml),
 * which exposes stack traces, actuator endpoints, and H2 console.
 *
 * To demo the vulnerability:
 *   java -jar target/owasp-top10-demo-*.jar --spring.profiles.active=vulnerable
 *   Then visit: /actuator/env (exposes secrets), /h2-console (database access)
 *
 * To demo the secure version:
 *   java -jar target/owasp-top10-demo-*.jar  (default profile)
 *   Then visit: /actuator/env -> 404
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        // GOOD: Never expose internal error details to the client
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "An unexpected error occurred",
                        "timestamp", Instant.now().toString(),
                        "status", 500
                        // No stack trace, no exception class name, no message
                ));
    }
}
