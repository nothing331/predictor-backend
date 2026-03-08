package core.ratelimit;

import java.time.Instant;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;
    private final MeterRegistry meterRegistry;
    private final DefaultRedisScript<Long> script;

    public RateLimiterService(StringRedisTemplate redisTemplate, RateLimiterProperties properties,
            MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;

        // Lua script for atomic increment and check
        String luaScript = """
                local c=redis.call('INCR',KEYS[1])
                if c==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end
                if c>tonumber(ARGV[2]) then return 0 end
                return 1
                """;
        this.script = new DefaultRedisScript<>(luaScript, Long.class);
    }

    public void guard(HttpServletRequest request, String userId, String ip) {
        String identity = (userId != null && !userId.isBlank()) ? userId : ip;
        // e.g. /v1/markets/123/trades -> trade
        String routeGroup = "trade";
        int maxReq = properties.getTrade().getMax();
        int windowSec = properties.getWindowSeconds();

        long epochWindow = Instant.now().getEpochSecond() / windowSec;
        String key = "rl:%s:%s:%d".formatted(routeGroup, identity, epochWindow);

        boolean allowed = allow(key, windowSec, maxReq);
        recordMetrics(routeGroup, allowed);

        if (!allowed) {
            throw new RateLimitExceededException("Too many requests for trade operations");
        }
    }

    public void guardSseConnect(String userId, String ip) {
        String identity = (userId != null && !userId.isBlank()) ? userId : ip;
        String routeGroup = "sse-connect";
        int maxReq = properties.getSseConnect().getMax();
        int windowSec = properties.getWindowSeconds();

        long epochWindow = Instant.now().getEpochSecond() / windowSec;
        String key = "rl:%s:%s:%d".formatted(routeGroup, identity, epochWindow);

        boolean allowed = allow(key, windowSec, maxReq);
        recordMetrics(routeGroup, allowed);

        if (!allowed) {
            throw new RateLimitExceededException("Too many SSE connect requests");
        }
    }

    private boolean allow(String key, int windowSec, int maxReq) {
        try {
            Long result = redisTemplate.execute(script, Collections.singletonList(key),
                    String.valueOf(windowSec), String.valueOf(maxReq));
            return result != null && result == 1L;
        } catch (RedisConnectionFailureException | org.springframework.data.redis.RedisSystemException e) {
            log.error("Redis error during rate limit check: {}", e.getMessage());
            if (properties.isFailClosed()) {
                throw new RateLimitExceededException("Rate limiter unavailable (fail-closed)");
            }
            // Fail open
            return true;
        } catch (Exception e) {
            log.error("Unexpected error during rate limit check: {}", e.getMessage(), e);
            if (properties.isFailClosed()) {
                throw new RateLimitExceededException("Rate limiter unavailable (fail-closed)");
            }
            // Fail open
            return true;
        }
    }

    private void recordMetrics(String route, boolean allowed) {
        if (meterRegistry != null) {
            String counterName = allowed ? "ratelimit.allowed" : "ratelimit.blocked";
            meterRegistry.counter(counterName, "route", route).increment();
        }
    }
}
