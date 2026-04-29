package com.pfip.authservice.service;

import com.pfip.authservice.config.CacheConfig;
import com.pfip.authservice.security.JwtUtil;
import com.pfip.common.dto.ValidationResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationService {

    private final JwtUtil jwtUtil;
    private final BlacklistService blacklistService;
    private final CacheManager caffeineCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;


    public ValidationResponse validate(String token, String callerService) {
        log.debug("Validation request from service: {}", callerService);

        // ── Layer 1: Caffeine L1 cache ─────────────────────────────────────
        String cacheKey = buildCacheKey(token);
        Cache caffeineCache = caffeineCacheManager.getCache(CacheConfig.CAFFEINE_CACHE);

        if (caffeineCache != null) {
            ValidationResponse cached = caffeineCache.get(cacheKey, ValidationResponse.class);
            if (cached != null) {
                log.debug("L1 Caffeine cache HIT for caller: {}", callerService);
                // Even on cache hit, check blacklist — blacklisting is real-time
                if (cached.isValid()) {
                    String tokenId = safeExtractTokenId(token);
                    if (tokenId != null && blacklistService.isBlacklisted(tokenId)) {
                        log.warn("Blacklisted token presented by caller: {}", callerService);
                        return buildInvalidResponse("Token has been revoked");
                    }
                }
                return cached;
            }
        }

        // ── Layer 2: Redis L2 cache ────────────────────────────────────────
        try {
            Object redisResult = redisTemplate.opsForValue()
                    .get(CacheConfig.TOKEN_CACHE_KEY_PREFIX + cacheKey);
            if (redisResult instanceof ValidationResponse redisResponse) {
                log.debug("L2 Redis cache HIT for caller: {}", callerService);
                // Populate L1 from L2
                if (caffeineCache != null) {
                    caffeineCache.put(cacheKey, redisResponse);
                }
                // Still check blacklist on Redis hit
                if (redisResponse.isValid()) {
                    String tokenId = safeExtractTokenId(token);
                    if (tokenId != null && blacklistService.isBlacklisted(tokenId)) {
                        return buildInvalidResponse("Token has been revoked");
                    }
                }
                return redisResponse;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for cache lookup — falling through to crypto: {}", e.getMessage());
        }

        // ── Layer 3: Full cryptographic validation ─────────────────────────
        log.debug("Cache MISS — performing cryptographic validation for caller: {}", callerService);
        ValidationResponse result = performCryptoValidation(token, callerService);

        // Store result in both caches
        storeInCaches(cacheKey, result, token);

        return result;
    }

    private ValidationResponse performCryptoValidation(String token, String callerService) {
        try {
            // Step 1: Verify signature and expiry
            if (!jwtUtil.validateToken(token)) {
                return buildInvalidResponse("Token signature invalid or expired");
            }

            // Step 2: Extract claims
            String username  = jwtUtil.extractUsername(token);
            Long userId      = jwtUtil.extractUserId(token);
            String role      = jwtUtil.extractRole(token);
            LocalDateTime exp = jwtUtil.extractExpiration(token);
            String tokenId   = jwtUtil.extractTokenId(token);

            // Step 3: Check blacklist (Redis lookup ~0.5ms)
            if (tokenId != null && blacklistService.isBlacklisted(tokenId)) {
                log.warn("Blacklisted token presented. Username: {}, caller: {}",
                        username, callerService);
                return buildInvalidResponse("Token has been revoked");
            }

            log.debug("Token valid for user: {}, caller: {}", username, callerService);

            return ValidationResponse.builder()
                    .valid(true)
                    .userId(userId)
                    .username(username)
                    .role(role)
                    .expiresAt(exp)
                    .build();

        } catch (ExpiredJwtException e) {
            log.warn("Expired token presented to caller: {}", callerService);
            return buildInvalidResponse("Token has expired");
        } catch (JwtException e) {
            log.warn("Invalid token from caller {}: {}", callerService, e.getMessage());
            return buildInvalidResponse("Token is invalid");
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", e.getMessage(), e);
            return buildInvalidResponse("Validation error");
        }
    }

    private void storeInCaches(String cacheKey, ValidationResponse result, String token) {
        // Store in Caffeine L1
        Cache caffeineCache = caffeineCacheManager.getCache(CacheConfig.CAFFEINE_CACHE);
        if (caffeineCache != null) {
            caffeineCache.put(cacheKey, result);
        }

        // Store in Redis L2 — only cache valid tokens
        // Invalid tokens have no TTL value to use, so we skip caching them
        if (result.isValid() && result.getExpiresAt() != null) {
            try {
                Duration ttl = Duration.between(LocalDateTime.now(), result.getExpiresAt());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(
                            CacheConfig.TOKEN_CACHE_KEY_PREFIX + cacheKey,
                            result,
                            ttl);
                }
            } catch (Exception e) {
                log.warn("Failed to store in Redis L2 cache — continuing: {}", e.getMessage());
            }
        }
    }

    /**
     * Evict a token from both caches on logout or password change.
     */
    public void evictFromCaches(String token) {
        String cacheKey = buildCacheKey(token);
        Cache caffeineCache = caffeineCacheManager.getCache(CacheConfig.CAFFEINE_CACHE);
        if (caffeineCache != null) {
            caffeineCache.evict(cacheKey);
        }
        try {
            redisTemplate.delete(CacheConfig.TOKEN_CACHE_KEY_PREFIX + cacheKey);
        } catch (Exception e) {
            log.warn("Failed to evict from Redis: {}", e.getMessage());
        }
    }

    private String buildCacheKey(String token) {

        if (token.length() <= 32) return token;
        return token.substring(token.length() - 32);
    }

    private String safeExtractTokenId(String token) {
        try {
            return jwtUtil.extractTokenId(token);
        } catch (Exception e) {
            return null;
        }
    }

    private ValidationResponse buildInvalidResponse(String reason) {
        return ValidationResponse.builder()
                .valid(false)
                .reason(reason)
                .build();
    }
}