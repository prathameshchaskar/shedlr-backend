package com.shedlr.authservice.common.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * RedisConfig handles the connection to Redis for distributed state management.
 * In this project, Redis is primarily used as the backend for Bucket4j's rate limiting,
 * ensuring that request quotas are shared across all instances of the microservice.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    /**
     * Creates a RedisClient using the configured host and port.
     * Lettuce is used as the underlying driver for high-performance, non-blocking I/O.
     */
    @Bean
    public RedisClient redisClient() {
        return RedisClient.create(RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .build());
    }

    /**
     * Configures the Bucket4j ProxyManager using the Redis Lettuce extension.
     * This manager handles the distribution of bucket states across the Redis cluster.
     * 
     * We use a ByteArrayCodec for compatibility with Bucket4j's internal serialization
     * of bucket state.
     */
    @Bean
    public ProxyManager<String> proxyManager(RedisClient redisClient) {
        // We connect using a composite codec: String for the key, and Byte Array for the value (bucket state)
        return LettuceBasedProxyManager.builderFor(redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)))
                .withExpirationStrategy(ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofMinutes(10)))
                .build();
    }
}
