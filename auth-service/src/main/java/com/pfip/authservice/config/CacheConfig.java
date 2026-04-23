package com.pfip.authservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String CAFFEINE_CACHE = "caffeineTokenCache";
    public static final String BLACKLIST_KEY_PREFIX = "blacklist:";

    @Bean
    @Primary
    public CacheManager caffeineCacheManager(){
        CaffeineCacheManager manager = new CaffeineCacheManager(CAFFEINE_CACHE);
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(10_000)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats());
        return manager;
    }
}
