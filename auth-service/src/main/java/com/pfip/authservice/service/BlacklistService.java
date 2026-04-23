package com.pfip.authservice.service;

import com.pfip.authservice.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void blecklistToken (String tokenId, LocalDateTime expiresAt){
        Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) return; //already expired hence blackListed

        String key = CacheConfig.BLACKLIST_KEY_PREFIX + tokenId;
    }
}
