package com.owaspdemo.a09_logging_failures;

import com.owaspdemo.common.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * A09:2025 - Security Logging and Alerting Failures
 *
 * VULNERABLE: Login attempts are not logged at all.
 * Failed logins, brute-force attacks, and privilege escalations leave no trace.
 *
 * Try: POST /api/v1/vulnerable/login with wrong credentials 100 times
 * Check logs: nothing recorded — the attack is invisible.
 */
@RestController
@RequestMapping("/api/v1/vulnerable")
public class VulnerableLoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public VulnerableLoginController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        var user = userRepository.findByUsername(username);

        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPasswordHash())) {
            // BAD: Successful login — no audit log
            System.out.println("User logged in");  // BAD: System.out, no structure, no context
            return Map.of("authenticated", true, "username", username);
        }

        // BAD: Failed login — completely silent. No log, no alert, no counter.
        return Map.of("authenticated", false);
    }
}
