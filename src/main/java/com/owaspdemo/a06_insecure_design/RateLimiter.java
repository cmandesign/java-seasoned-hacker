package com.owaspdemo.a06_insecure_design;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AtomicInteger count = attempts.get(key);
        return count != null && count.get() >= MAX_ATTEMPTS;
    }

    public void recordAttempt(String key) {
        attempts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    public int getAttempts(String key) {
        AtomicInteger count = attempts.get(key);
        return count == null ? 0 : count.get();
    }
}
