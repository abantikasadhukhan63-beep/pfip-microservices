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

    public void blacklistToken (String tokenId, LocalDateTime expiresAt){
        Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) return; //already expired hence blackListed

        String key = CacheConfig.BLACKLIST_KEY_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", ttl);
        log.info("Token blacklisted: {}, TTL: {}s", tokenId, ttl.getSeconds());
    }

    /**
     * Check if a token ID is already blacklisted.
     * This is called on every validation.
     */

    public boolean isBlacklisted(String tokenId){
        if (tokenId == null) return false;
        String key = CacheConfig.BLACKLIST_KEY_PREFIX + tokenId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        }
        catch (Exception e){
            log.error("Redis unavailable for blacklist check - failing open: {}", e.getMessage());
            return false;
        }
    }
}
