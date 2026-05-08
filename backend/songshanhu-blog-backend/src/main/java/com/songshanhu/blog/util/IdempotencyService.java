package com.songshanhu.blog.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            return true;
        }
        Boolean ok = redisTemplate.opsForValue().setIfAbsent("idem:" + key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }
}

