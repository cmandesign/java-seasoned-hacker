package com.owaspdemo.a04_cryptographic_failures;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * VULNERABLE: MD5 password encoder — intentionally insecure for OWASP demo purposes.
 *
 * Why this is dangerous:
 * - MD5 is fast (~billions hashes/sec on GPU), making brute-force trivial
 * - No salt: identical passwords produce identical hashes (rainbow table attacks)
 * - Cryptographically broken: collision attacks are practical
 *
 * Use BCryptPasswordEncoder, SCryptPasswordEncoder, or Argon2PasswordEncoder instead.
 */
public class MD5PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(rawPassword.toString().getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encode(rawPassword).equals(encodedPassword);
    }
}
