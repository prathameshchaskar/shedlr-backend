package com.shedlr.authservice.identity.security;

import com.shedlr.authservice.common.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * RateLimitingService provides brute-force protection using Bucket4j.
 * It uses Redis (via ProxyManager) to store bucket state, ensuring it works in a distributed environment.
 * 
 * By storing the 'buckets' in Redis, we prevent attackers from bypassing rate limits by hitting 
 * different instances of our microservice.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingService {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;

    /**
     * Attempts to consume a token for a given key (e.g., IP address or email).
     *
     * @param key The unique key to identify the requester (IP Address or Username).
     * @return true if a token was consumed (request allowed), false if the limit was exceeded (429 Too Many Requests).
     */
    public boolean tryConsume(String key) {
        // Build or retrieve the bucket for this specific key from Redis
        Bucket bucket = proxyManager.builder().build(key, getBucketConfiguration());
        return bucket.tryConsume(1);
    }

    /**
     * Attempts to consume a token for a given key with a specific configuration.
     *
     * @param key The unique key to identify the requester.
     * @param limit The maximum number of tokens.
     * @param period The refill period.
     * @return true if a token was consumed, false if exceeded.
     */
    public boolean tryConsume(String key, int limit, Duration period) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.classic(limit, Refill.intervally(limit, period)))
                .build();
        Bucket bucket = proxyManager.builder().build(key, () -> configuration);
        return bucket.tryConsume(1);
    }

    /**
     * Defines the rate limit configuration using the Token Bucket algorithm.
     * 
     * Strategy:
     * - Bandwidth: Limit to 5 requests.
     * - Refill: Replenish 5 tokens every 1 minute.
     * 
     * This allows for short bursts of up to 5 requests, but maintains a long-term average 
     * of 5 requests per minute.
     *
     * @return BucketConfiguration
     */
    private Supplier<BucketConfiguration> getBucketConfiguration() {
        return () -> {
            RateLimitProperties.LimitConfig login = rateLimitProperties.getLogin();
            return BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(login.getCapacity(), 
                            Refill.intervally(login.getCapacity(), login.getPeriod())))
                    .build();
        };
    }
}
