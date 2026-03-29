package com.owaspdemo.a06_insecure_design;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public record OtpSession(String otp, Instant expiresAt) {}

    private final Map<String, OtpSession> sessions = new ConcurrentHashMap<>();

    /** Generate a 4-digit OTP (vulnerable — easy to brute force) */
    public String generateWeak(String sessionId) {
        String otp = String.format("%04d", RANDOM.nextInt(10_000));
        sessions.put(sessionId, new OtpSession(otp, Instant.MAX)); // no expiry
        return otp;
    }

    /** Generate a 6-digit OTP with 5-minute TTL (secure) */
    public String generateStrong(String sessionId) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        sessions.put(sessionId, new OtpSession(otp, Instant.now().plusSeconds(300)));
        return otp;
    }

    /** Verify without any security checks */
    public boolean verifyWeak(String sessionId, String guess) {
        OtpSession session = sessions.get(sessionId);
        if (session == null) return false;
        return session.otp().equals(guess); // BAD: timing-based string comparison
    }

    /** Verify with expiry + constant-time comparison */
    public boolean verifyStrong(String sessionId, String guess) {
        OtpSession session = sessions.get(sessionId);
        if (session == null) return false;
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(sessionId);
            return false;
        }
        return java.security.MessageDigest.isEqual(
                session.otp().getBytes(), guess.getBytes());
    }

    public void invalidate(String sessionId) {
        sessions.remove(sessionId);
    }
}
