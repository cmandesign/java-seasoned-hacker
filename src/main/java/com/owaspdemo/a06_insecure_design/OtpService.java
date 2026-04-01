package com.owaspdemo.a06_insecure_design;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private static final String WEAK_KEY_PREFIX = "otp:weak:";
    private static final String STRONG_KEY_PREFIX = "otp:strong:";
    private static final Duration STRONG_OTP_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** Generate a 4-digit OTP (vulnerable — easy to brute force, no expiry) */
    public String generateWeak(String sessionId) {
        String otp = String.format("%04d", RANDOM.nextInt(10_000));
        redisTemplate.opsForValue().set(WEAK_KEY_PREFIX + sessionId, otp); // no TTL
        return otp;
    }

    /** Generate a 6-digit OTP with 5-minute TTL (secure) */
    public String generateStrong(String sessionId) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(STRONG_KEY_PREFIX + sessionId, otp, STRONG_OTP_TTL);
        return otp;
    }

    /** Verify without any security checks */
    public boolean verifyWeak(String sessionId, String guess) {
        String stored = redisTemplate.opsForValue().get(WEAK_KEY_PREFIX + sessionId);
        if (stored == null) return false;
        return stored.equals(guess); // BAD: timing-based string comparison
    }

    /** Verify with expiry (via Redis TTL) + constant-time comparison */
    public boolean verifyStrong(String sessionId, String guess) {
        String stored = redisTemplate.opsForValue().get(STRONG_KEY_PREFIX + sessionId);
        if (stored == null) return false; // not found or expired
        return java.security.MessageDigest.isEqual(
                stored.getBytes(), guess.getBytes());
    }

    public void invalidate(String sessionId) {
        redisTemplate.delete(STRONG_KEY_PREFIX + sessionId);
        redisTemplate.delete(WEAK_KEY_PREFIX + sessionId);
    }
}
