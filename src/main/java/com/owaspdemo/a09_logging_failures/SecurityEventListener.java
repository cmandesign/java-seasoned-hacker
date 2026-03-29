package com.owaspdemo.a09_logging_failures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A09:2025 - Security Logging and Alerting Failures
 *
 * SECURE: Listens to security events and logs them with structured context (MDC).
 * Triggers an alert after 3 failed logins from the same IP in the session.
 */
@Component
public class SecurityEventListener {

    // Using the "secure" logger that goes to both CONSOLE and SECURITY_AUDIT file
    private static final Logger log = LoggerFactory.getLogger("com.owaspdemo.a09_logging_failures.secure");

    private final ConcurrentHashMap<String, AtomicInteger> failuresByIp = new ConcurrentHashMap<>();

    @EventListener
    public void onSecurityEvent(SecurityAuditEvent event) {
        MDC.put("action", event.getAction().name());
        MDC.put("userId", event.getUsername());
        MDC.put("sourceIp", event.getSourceIp());
        MDC.put("outcome", event.getAction() == SecurityAuditEvent.Action.LOGIN_SUCCESS ? "SUCCESS" : "FAILURE");

        try {
            log.info("Security event: {} | user={} | ip={} | {}",
                    event.getAction(), event.getUsername(), event.getSourceIp(), event.getDetails());

            if (event.getAction() == SecurityAuditEvent.Action.LOGIN_FAILURE) {
                int failures = failuresByIp
                        .computeIfAbsent(event.getSourceIp(), k -> new AtomicInteger(0))
                        .incrementAndGet();

                if (failures >= 3) {
                    MDC.put("outcome", "ALERT");
                    log.warn("ALERT: {} failed login attempts from IP {} — possible brute force attack",
                            failures, event.getSourceIp());
                }
            }

            if (event.getAction() == SecurityAuditEvent.Action.LOGIN_SUCCESS) {
                failuresByIp.remove(event.getSourceIp());
            }
        } finally {
            MDC.clear();
        }
    }
}
