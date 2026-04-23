package com.pfip.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationService {

    private final BlacklistService blacklistService;
    private final CacheManager caffineCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
}
