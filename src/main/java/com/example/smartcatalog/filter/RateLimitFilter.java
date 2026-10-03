package com.example.smartcatalog.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Servlet filter that enforces per-identity rate limits using Bucket4j backed by Redis.
 *
 * <h3>Identity resolution (in priority order)</h3>
 * <ol>
 *   <li><strong>Authenticated user</strong> — uses the Spring Security principal name
 *       (i.e. the username / user ID stored in the JWT subject claim). This gives
 *       every registered user their own 20-request-per-minute quota regardless of which
 *       IP they connect from.</li>
 *   <li><strong>Unauthenticated request</strong> — falls back to the client IP address.
 *       The filter honours the {@code X-Forwarded-For} header so that requests passing
 *       through a reverse-proxy / load-balancer are attributed to the original client.</li>
 * </ol>
 *
 * <h3>Bucket configuration</h3>
 * <ul>
 *   <li>Capacity: <strong>20 tokens</strong></li>
 *   <li>Refill: <strong>20 tokens every 1 minute</strong> (greedy — all tokens
 *       become available at once when the window resets)</li>
 * </ul>
 *
 * <h3>Response headers</h3>
 * The filter adds informational rate-limit headers to every response:
 * <ul>
 *   <li>{@code X-RateLimit-Remaining} — tokens left in the current window</li>
 *   <li>{@code X-RateLimit-Retry-After-Seconds} — seconds until the next token is
 *       available (only present on HTTP 429 responses)</li>
 * </ul>
 *
 * <h3>HTTP 429</h3>
 * When the bucket is exhausted the filter short-circuits the filter chain and writes
 * a JSON error body consistent with the rest of the API's {@code ApiResponse} shape.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Maximum number of requests allowed per time window. */
    private static final int RATE_LIMIT_CAPACITY = 20;

    /** Duration of the refill window. */
    private static final Duration RATE_LIMIT_PERIOD = Duration.ofMinutes(1);

    /** Redis key prefix — prevents collisions with other keys in the same database. */
    private static final String KEY_PREFIX = "rate_limit:";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final LettuceBasedProxyManager<byte[]> rateLimitProxyManager;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Bucket configuration supplier (stateless, shared across all buckets)
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link Supplier} that produces a fresh {@link BucketConfiguration}
     * on demand. Bucket4j calls this supplier only once per key — when the bucket
     * does not yet exist in Redis.
     *
     * <p>Uses a <em>greedy</em> (fixed-window) refill: all 20 tokens are restored
     * at once when the 1-minute window expires.
     */
    private Supplier<BucketConfiguration> bucketConfigurationSupplier() {
        return () -> BucketConfiguration.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(RATE_LIMIT_CAPACITY)
                                .refillGreedy(RATE_LIMIT_CAPACITY, RATE_LIMIT_PERIOD)
                                .build()
                )
                .build();
    }

    // -------------------------------------------------------------------------
    // Filter logic
    // -------------------------------------------------------------------------

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String rateLimitKey = resolveRateLimitKey(request);
        byte[] redisKey = (KEY_PREFIX + rateLimitKey).getBytes();

        // Resolve (or lazily create) the bucket for this identity
        Bucket bucket = rateLimitProxyManager
                .builder()
                .build(redisKey, bucketConfigurationSupplier());

        // Try to consume 1 token — non-blocking
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        // Always expose remaining tokens so clients can self-throttle
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            log.debug("Rate limit OK for '{}' — {} token(s) remaining", rateLimitKey, probe.getRemainingTokens());
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            log.warn("Rate limit exceeded for '{}' — retry after {}s", rateLimitKey, retryAfterSeconds);

            response.setHeader("X-RateLimit-Retry-After-Seconds", String.valueOf(retryAfterSeconds));
            writeTooManyRequestsResponse(response, retryAfterSeconds);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Determines the rate-limit key for the current request.
     *
     * <p>Priority:
     * <ol>
     *   <li>Authenticated principal name (username) from the Spring Security context</li>
     *   <li>The value of the {@code X-Forwarded-For} header (first IP in the chain)</li>
     *   <li>The raw remote address from the servlet request</li>
     * </ol>
     */
    private String resolveRateLimitKey(HttpServletRequest request) {
        // 1. Prefer the authenticated user identity
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }

        // 2. Honour X-Forwarded-For for requests behind a proxy / load balancer
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take only the first (original) client IP
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }

        // 3. Direct connection — use the raw remote address
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Writes a JSON HTTP 429 response that matches the project's
     * {@code ApiResponse} envelope shape.
     *
     * <pre>
     * {
     *   "status_code": 429,
     *   "message": "Too many requests — you have exceeded the rate limit of 20 requests per minute.",
     *   "error": {
     *     "code": "RATE_LIMIT_EXCEEDED",
     *     "retry_after_seconds": 42
     *   },
     *   "body": null
     * }
     * </pre>
     */
    private void writeTooManyRequestsResponse(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Build a response body consistent with ApiResponse<T> shape
        Map<String, Object> body = Map.of(
                "status_code", HttpStatus.TOO_MANY_REQUESTS.value(),
                "message", "Too many requests — you have exceeded the rate limit of "
                        + RATE_LIMIT_CAPACITY + " requests per minute.",
                "error", Map.of(
                        "code", "RATE_LIMIT_EXCEEDED",
                        "retry_after_seconds", retryAfterSeconds
                ),
                "body", ""  // Map.of does not allow null values; empty string signals absence
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
