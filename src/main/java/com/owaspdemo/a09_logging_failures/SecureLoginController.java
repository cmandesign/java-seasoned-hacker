package com.owaspdemo.a09_logging_failures;

import com.owaspdemo.common.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A09:2025 - Security Logging and Alerting Failures
 *
 * SECURE: Every login attempt is logged with structured context.
 * After 3 failed attempts from the same IP, an alert is triggered.
 *
 * Try: POST /api/v1/secure/login with wrong credentials 4 times
 * Check logs/security-audit.log: all attempts recorded, alert triggered after 3rd.
 */
@RestController
@RequestMapping("/api/v1/secure")
public class SecureLoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public SecureLoginController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                 ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        String clientIp = request.getRemoteAddr();

        var user = userRepository.findByUsername(username);

        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPasswordHash())) {
            // GOOD: Log successful authentication with context
            eventPublisher.publishEvent(new SecurityAuditEvent(
                    this, SecurityAuditEvent.Action.LOGIN_SUCCESS,
                    username, clientIp, "Successful login"));

            return Map.of("authenticated", true, "username", username);
        }

        // GOOD: Log failed attempt — enables brute-force detection
        eventPublisher.publishEvent(new SecurityAuditEvent(
                this, SecurityAuditEvent.Action.LOGIN_FAILURE,
                username != null ? username : "unknown", clientIp,
                "Failed login attempt — invalid credentials"));

        return Map.of("authenticated", false);
    }
}
