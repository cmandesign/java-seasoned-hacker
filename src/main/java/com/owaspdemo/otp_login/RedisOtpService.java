package com.owaspdemo.otp_login;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class RedisOtpService {

    private static final String OTP_KEY_PREFIX = "otp:login:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public RedisOtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateAndStore(String username) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(OTP_KEY_PREFIX + username, otp, OTP_TTL);
        return otp;
    }

    public boolean verify(String username, String otp) {
        String key = OTP_KEY_PREFIX + username;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return false;
        }
        boolean match = java.security.MessageDigest.isEqual(
                stored.getBytes(), otp.getBytes());
        if (match) {
            redisTemplate.delete(key);
        }
        return match;
    }
}
