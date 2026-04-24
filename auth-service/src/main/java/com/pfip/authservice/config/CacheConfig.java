package com.pfip.authservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * L1 cache, not shared across instances
 */

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

    /**
     * L2-Redis shared cache.
     * Survives restarts
     * Used for token validation
     */

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory){
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(12))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer((new GenericJackson2JsonRedisSerializer())));

        return  RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Redis template for direct Redis operations (blacklist, manual cache ops)
     */

    @Bean
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
