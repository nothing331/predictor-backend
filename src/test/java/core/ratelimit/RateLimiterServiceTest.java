package core.ratelimit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;

public class RateLimiterServiceTest {

    private StringRedisTemplate redisTemplate;
    private RateLimiterProperties properties;
    private MeterRegistry meterRegistry;
    private RateLimiterService rateLimiterService;
    private HttpServletRequest request;

    @BeforeEach
    public void setup() {
        redisTemplate = mock(StringRedisTemplate.class);
        properties = new RateLimiterProperties();

        // Mock properties
        RateLimiterProperties.Trade tradeProps = new RateLimiterProperties.Trade();
        tradeProps.setMax(5);
        properties.getTrade().setMax(5);

        RateLimiterProperties.SseConnect sseProps = new RateLimiterProperties.SseConnect();
        sseProps.setMax(10);
        properties.getSseConnect().setMax(10);

        properties.setWindowSeconds(60);
        properties.setFailClosed(true);

        meterRegistry = mock(MeterRegistry.class);
        Counter counter = mock(Counter.class);
        // properly mock meterRegistry counter so it doesn't throw NPE
        when(meterRegistry.counter(any(String.class), any(String.class), any(String.class))).thenReturn(counter);

        rateLimiterService = new RateLimiterService(redisTemplate, properties, meterRegistry);
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void testGuardAllowed() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class), any(String.class)))
                .thenReturn(1L);

        assertDoesNotThrow(() -> rateLimiterService.guard(request, "user1", "127.0.0.1"));
        verify(meterRegistry).counter("ratelimit.allowed", "route", "trade");
    }

    @Test
    public void testGuardBlocked() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class), any(String.class)))
                .thenReturn(0L);

        assertThrows(RateLimitExceededException.class, () -> rateLimiterService.guard(request, "user1", "127.0.0.1"));
        verify(meterRegistry).counter("ratelimit.blocked", "route", "trade");
    }

    @Test
    public void testGuardSseAllowed() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class), any(String.class)))
                .thenReturn(1L);

        assertDoesNotThrow(() -> rateLimiterService.guardSseConnect("user1", "127.0.0.1"));
        verify(meterRegistry).counter("ratelimit.allowed", "route", "sse-connect");
    }

    @Test
    public void testRedisFailureFailClosed() {
        properties.setFailClosed(true);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class), any(String.class)))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        assertThrows(RateLimitExceededException.class, () -> rateLimiterService.guard(request, "user1", "127.0.0.1"));
    }

    @Test
    public void testRedisFailureFailOpen() {
        properties.setFailClosed(false);
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(String.class), any(String.class)))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));

        assertDoesNotThrow(() -> rateLimiterService.guard(request, "user1", "127.0.0.1"));
    }
}
