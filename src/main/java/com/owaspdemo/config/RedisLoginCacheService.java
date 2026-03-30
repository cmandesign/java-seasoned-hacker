package com.owaspdemo.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLoginCacheService {

    private static final String KEY_PREFIX = "login:token:";
    private static final Duration TTL = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;

    public RedisLoginCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheLoginToken(String username, String token) {
        redisTemplate.opsForValue().set(KEY_PREFIX + username, token, TTL);
    }

    public String getCachedToken(String username) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + username);
    }

    public void removeCachedToken(String username) {
        redisTemplate.delete(KEY_PREFIX + username);
    }
}
