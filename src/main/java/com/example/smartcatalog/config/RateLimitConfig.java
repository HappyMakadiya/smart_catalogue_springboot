package com.example.smartcatalog.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configures the Bucket4j distributed rate-limiting infrastructure backed by Redis.
 *
 * <p>Uses the Lettuce Redis client (already on the classpath via
 * {@code spring-boot-starter-data-redis}) to create a
 * {@link LettuceBasedProxyManager}. Each unique key (user ID or client IP) maps
 * to its own {@code Bucket} stored atomically in Redis, so rate limits are
 * enforced consistently across all application instances.
 *
 * <p>The expiration strategy ensures that idle buckets are automatically evicted
 * from Redis once they have fully refilled, preventing unbounded memory growth.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Redis URI injected from {@code application.properties}.
     * Falls back to {@code redis://localhost:6379} in local development.
     */
    @Value("${spring.data.redis.url:redis://localhost:6379}")
    private String redisUrl;

    /**
     * Standalone Lettuce {@link RedisClient} used exclusively by Bucket4j.
     *
     * <p>A separate client (rather than reusing Spring's auto-configured
     * {@code RedisConnectionFactory}) is preferred because Bucket4j's proxy
     * manager needs direct access to the low-level Lettuce {@code StatefulRedisConnection}.
     */
    @Bean(destroyMethod = "shutdown")
    public RedisClient bucket4jRedisClient() {
        return RedisClient.create(redisUrl);
    }

    /**
     * {@link LettuceBasedProxyManager} — the central Bucket4j component that
     * stores and retrieves bucket state from Redis using compare-and-swap (CAS)
     * semantics.
     *
     * <p>The expiration strategy keeps each bucket's Redis key alive for the
     * duration needed to refill it to its maximum capacity (1 minute for a
     * 20 req/min bucket), so keys for inactive clients are cleaned up automatically.
     *
     * @param redisClient the Lettuce client created above
     * @return configured proxy manager
     */
    @Bean
    public LettuceBasedProxyManager<byte[]> rateLimitProxyManager(RedisClient redisClient) {
        return LettuceBasedProxyManager.builderFor(redisClient)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                                Duration.ofMinutes(1) // match the refill period
                        )
                )
                .build();
    }
}
