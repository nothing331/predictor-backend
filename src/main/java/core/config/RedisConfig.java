package core.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import io.lettuce.core.SocketOptions;
import io.lettuce.core.ClientOptions;

/**
 * Redis client configuration tuned for the Market read cache.
 *
 * <p>Two non-default behaviors:
 * <ul>
 *   <li><b>200ms connect + command timeout.</b> The default Lettuce timeouts
 *       (60s) would make a Redis outage tank every read endpoint instead of
 *       just removing the cache. With 200ms, a Redis-down request costs
 *       +200ms in the worst case before falling through to Postgres.</li>
 *   <li><b>{@code disconnected-behavior = REJECT_COMMANDS}.</b> If Lettuce
 *       knows it isn't connected, commands fail fast rather than queueing
 *       until they time out.</li>
 * </ul>
 *
 * <p>Conditional on {@code spring.data.redis.url} being present. When the
 * property is absent (typical in unit tests), no Redis beans are created and
 * {@link core.cache.MarketReadCache} silently no-ops.
 *
 * <p>See {@code docs/adr/0005-redis-read-cache-cache-aside-delete-after-commit.md}.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.url")
public class RedisConfig {

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.url}") String redisUrl) {
        URI uri = URI.create(redisUrl);
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        if (uri.getHost() != null) standalone.setHostName(uri.getHost());
        if (uri.getPort() > 0)     standalone.setPort(uri.getPort());
        if (uri.getUserInfo() != null) {
            String[] creds = uri.getUserInfo().split(":", 2);
            if (creds.length == 2) standalone.setPassword(creds[1]);
        }

        Duration shortTimeout = Duration.ofMillis(200);
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(shortTimeout)
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(shortTimeout)
                                .build())
                        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                        .build())
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
        factory.setValidateConnection(false);
        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
