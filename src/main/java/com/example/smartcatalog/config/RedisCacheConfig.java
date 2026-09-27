package com.example.smartcatalog.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis cache configuration.
 *
 * <p>Registers a {@link RedisCacheManager} with:
 * <ul>
 *   <li>JSON serialization for values (human-readable, avoids Java serialization issues)</li>
 *   <li>A default TTL of <strong>1 hour</strong> applied to every cache unless overridden</li>
 *   <li>Per-cache TTL overrides via {@code cacheConfigurations} – add entries here
 *       whenever a specific cache needs a different expiry</li>
 * </ul>
 *
 * <p><b>Serializer note:</b> Uses {@link GenericJacksonJsonRedisSerializer} (Jackson 3),
 * the replacement for the deprecated {@code GenericJackson2JsonRedisSerializer}.
 * Default typing is explicitly enabled so that deserialization can reconstruct
 * the correct concrete types (e.g. {@code ProductDto}, {@code PageImpl}) from the
 * stored JSON without needing the target type at call-site.
 */
@Configuration
public class RedisCacheConfig {

    /**
     * Shared base configuration: JSON values with default typing, 1-hour TTL,
     * no caching of null values.
     *
     * <p>Default typing embeds {@code @class} metadata in the JSON so that
     * Spring can deserialize polymorphic / generic return types correctly
     * (e.g. {@code Page<ProductDto>}).</p>
     */
    private RedisCacheConfiguration defaultCacheConfig() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.smartcatalog.")
                .allowIfSubType("java.util.")
                .allowIfSubType("org.springframework.data.domain.")
                .build();

        // 2. Move activateDefaultTyping onto the Builder
        ObjectMapper mapper = JsonMapper.builder()
                .addMixIn(PageImpl.class, PageImplMixin.class)
                .addMixIn(PageRequest.class, PageRequestMixin.class)
                // 2. Explicitly set As.PROPERTY to embed "@class" inside the JSON object
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();

        // 3. Pass the custom mapper to the serializer
        GenericJacksonJsonRedisSerializer jsonSerializer =
                new GenericJacksonJsonRedisSerializer(mapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
    }

    /**
     * Builds and registers the {@link RedisCacheManager}.
     *
     * <p>Per-cache TTL overrides can be added to the {@code cacheConfigurations}
     * map below – for example, a shorter TTL for fast-changing data:</p>
     * <pre>
     *   Map.of("products", defaultCacheConfig().entryTtl(Duration.ofMinutes(30)))
     * </pre>
     *
     * @param connectionFactory auto-configured Lettuce connection factory
     * @return configured {@link RedisCacheManager}
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Per-cache TTL overrides (empty = every cache inherits the 1-hour default)
        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                // example override – uncomment and adjust as needed:
                // "products", defaultCacheConfig().entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig())
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class PageImplMixin<T> {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    PageImplMixin(@JsonProperty("content") List<T> content,
                  @JsonProperty("pageable") Pageable pageable,
                  @JsonProperty("totalElements") long total) {}
}

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class PageRequestMixin {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    static PageRequest of(@JsonProperty("pageNumber") int page,
                          @JsonProperty("pageSize") int size) {
        return null; // The body is ignored by Jackson
    }
}