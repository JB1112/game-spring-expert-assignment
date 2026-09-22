package com.gameexpert.chat.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of("""
        local key = KEYS[1]
        local limit = tonumber(ARGV[1])
        local ttl = tonumber(ARGV[2])

        local count = tonumber(redis.call('GET', key) or '0')
        if count >= limit then
            return 0
        end

        local updated = redis.call('INCR', key)
        if updated == 1 then
            redis.call('EXPIRE', key, ttl)
        end

        return 1
        """, Long.class);

    public boolean allow(Long playerId) {
        String key = "chat:limit:" + playerId;
        Long allowed = redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of(key), "5", "10");
        return allowed != null && allowed == 1L;
    }
}
