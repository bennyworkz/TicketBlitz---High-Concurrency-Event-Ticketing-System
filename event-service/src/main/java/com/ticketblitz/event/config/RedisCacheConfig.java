package com.ticketblitz.event.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis Cache Configuration
 * Configures Redis as the caching provider with Java 8 date/time support
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Configure ObjectMapper specifically for Redis caching
     * Note: This is separate from the default ObjectMapper used for HTTP requests
     */
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register Java 8 date/time module for Instant, LocalDateTime, etc.
        mapper.registerModule(new JavaTimeModule());
        
        // Enable default typing for polymorphic deserialization in Redis
        // This is needed to properly deserialize cached objects
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        
        return mapper;
    }

    /**
     * Configure Redis Cache Manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create serializer with custom ObjectMapper for Redis only
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());
        
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))  // Cache for 1 hour
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer)
                )
                .disableCachingNullValues();

        // Custom cache configurations
        RedisCacheConfiguration eventsConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(30));  // Events cache for 30 minutes

        RedisCacheConfiguration seatsConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(15));  // Seats cache for 15 minutes

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("events", eventsConfig)
                .withCacheConfiguration("seats", seatsConfig)
                .build();
    }
}
