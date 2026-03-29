package com.owaspdemo.a09_logging_failures;

import com.owaspdemo.common.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vulnerable")
@Tag(name = "A09 - Logging Failures", description = "Silent login vs structured audit logging with brute-force alerting")
public class VulnerableLoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public VulnerableLoginController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    @Operation(summary = "Login (no audit logging)", description = "Failed attempts leave no trace — brute-force attacks invisible")
    public Map<String, Object> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = "{\"username\": \"admin\", \"password\": \"wrong\"}")))
            @RequestBody Map<String, String> body) {
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
