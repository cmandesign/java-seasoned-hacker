package com.owaspdemo.a09_logging_failures;

import com.owaspdemo.common.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/secure")
@Tag(name = "A09 - Logging Failures")
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
    @Operation(summary = "Login (structured audit logging)", description = "Every attempt logged. Alert after 3 failures from same IP. Check logs/security-audit.log")
    public Map<String, Object> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"admin\", \"password\": \"wrong\"}")))
            @RequestBody Map<String, String> body, HttpServletRequest request) {
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
