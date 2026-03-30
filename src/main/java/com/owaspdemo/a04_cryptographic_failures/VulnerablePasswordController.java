package com.owaspdemo.a04_cryptographic_failures;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.model.Role;
import com.owaspdemo.common.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/vulnerable/password")
@Tag(name = "A04 - Cryptographic Failures",
     description = "MD5 password hashing vs BCrypt — demonstrates why weak hashing is dangerous")
public class VulnerablePasswordController {

    private final MD5PasswordEncoder md5Encoder = new MD5PasswordEncoder();
    private final UserRepository userRepository;

    public VulnerablePasswordController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @Operation(summary = "Register user with MD5-hashed password (vulnerable)",
               description = "Stores password as unsalted MD5 hash. "
                       + "Crack any hash instantly at https://crackstation.net or with hashcat -m 0.")
    public ResponseEntity<?> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            value = "{\"username\": \"md5user\", \"password\": \"Password1!\", "
                                  + "\"email\": \"md5user@example.com\"}")))
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");

        if (username == null || password == null || email == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "username, password, and email are required"));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists"));
        }

        String md5Hash = md5Encoder.encode(password);

        AppUser user = new AppUser(username, md5Hash, email, null, Role.USER,
                username, "", "");
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "User registered with MD5-hashed password",
                "username", username,
                "md5Hash", md5Hash,
                "warning", "This MD5 hash has no salt. Identical passwords produce identical hashes. "
                         + "Try registering two users with the same password and compare the hashes."
        ));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with MD5 password verification (vulnerable)",
               description = "Verifies password by comparing MD5 hashes. No rate limiting, no salt.")
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(
                            value = "{\"username\": \"md5user\", \"password\": \"Password1!\"}")))
            @RequestBody Map<String, String> body) {

        String username = body.get("username");
        String password = body.get("password");

        Optional<AppUser> user = userRepository.findByUsername(username);
        if (user.isEmpty() || !md5Encoder.matches(password, user.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }

        AppUser appUser = user.get();
        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "username", appUser.getUsername(),
                "role", appUser.getRole().name(),
                "storedHash", appUser.getPasswordHash(),
                "warning", "Password was verified using plain MD5. "
                         + "An attacker with DB access can crack these hashes in seconds."
        ));
    }

    @GetMapping("/hash")
    @Operation(summary = "Show MD5 hash of a password (educational)",
               description = "Returns the raw MD5 hash so you can see why it's insecure. "
                       + "Try hashing 'password' and look it up in a rainbow table.")
    public Map<String, String> hashPassword(@RequestParam String password) {
        String hash = md5Encoder.encode(password);
        return Map.of(
                "password", password,
                "md5Hash", hash,
                "hint", "Search this hash on crackstation.net — common passwords are instantly reversed."
        );
    }
}
