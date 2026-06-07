package core.cache;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import api.dto.GetAllMarket;

/**
 * Redis-backed read cache for Market list / detail DTOs.
 *
 * <p>Cache contract:
 * <ul>
 *   <li><b>List keys:</b> {@code market:list:all}, {@code market:list:OPEN},
 *       {@code market:list:RESOLVED} — TTL 3s.</li>
 *   <li><b>Detail keys:</b> {@code market:detail:{marketId}} — TTL 2s.</li>
 *   <li><b>Nothing else.</b> No users, positions, balances, trades.</li>
 * </ul>
 *
 * <p>Cache-aside pattern: read = check cache → on miss, query DB and populate.
 * Writes never go through here; the {@link MarketCacheInvalidator}
 * {@code @TransactionalEventListener} deletes affected keys after commit.
 *
 * <p>Redis failures are swallowed. Every method catches {@code RuntimeException}
 * and falls through (returns {@code null} on miss-or-error; no-ops on
 * put/invalidate-or-error). Combined with the 200ms client timeout in
 * {@link core.config.RedisConfig}, a Redis outage costs at most ~200ms per
 * request before the service falls back to Postgres.
 *
 * <p>{@code @ConditionalOnProperty(spring.data.redis.url)} — when Redis isn't
 * configured (e.g. unit-test profile) this bean isn't created. Callers must
 * handle a missing bean by getting it through {@link ObjectProvider}; see
 * {@link core.service.MarketService}.
 *
 * <p>See {@code docs/adr/0005-redis-read-cache-cache-aside-delete-after-commit.md}.
 */
@Component
@ConditionalOnProperty(name = "spring.data.redis.url")
public class MarketReadCache {

    private static final Logger log = LoggerFactory.getLogger(MarketReadCache.class);

    static final String LIST_KEY_PREFIX = "market:list:";
    static final String DETAIL_KEY_PREFIX = "market:detail:";
    static final List<String> ALL_LIST_KEYS = List.of(
            LIST_KEY_PREFIX + "all",
            LIST_KEY_PREFIX + "OPEN",
            LIST_KEY_PREFIX + "RESOLVED");

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration listTtl;
    private final Duration detailTtl;

    /** Rate-limited error log: warn at most once per 5s. */
    private final AtomicLong nextWarnLogMillis = new AtomicLong(0);
    private static final long WARN_THROTTLE_MILLIS = 5_000;

    public MarketReadCache(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        // Pinned values per ADR-0005 / Q8b.
        this.listTtl = Duration.ofSeconds(3);
        this.detailTtl = Duration.ofSeconds(2);
    }

    // ====================== READS ======================

    public List<GetAllMarket> getList(String statusQuery) {
        String key = listKey(statusQuery);
        return readValue(key, new TypeReference<List<GetAllMarket>>() {});
    }

    public GetAllMarket getDetail(String marketId) {
        return readValue(detailKey(marketId), new TypeReference<GetAllMarket>() {});
    }

    // ====================== WRITES (populate) ======================

    public void putList(String statusQuery, List<GetAllMarket> value) {
        writeValue(listKey(statusQuery), value, listTtl);
    }

    public void putDetail(String marketId, GetAllMarket value) {
        writeValue(detailKey(marketId), value, detailTtl);
    }

    // ====================== INVALIDATION ======================

    public void invalidateAllLists() {
        deleteKeys(ALL_LIST_KEYS);
    }

    public void invalidateDetail(String marketId) {
        deleteKeys(List.of(detailKey(marketId)));
    }

    public void invalidateMarket(String marketId) {
        deleteKeys(Set.of(
                detailKey(marketId),
                ALL_LIST_KEYS.get(0),
                ALL_LIST_KEYS.get(1),
                ALL_LIST_KEYS.get(2)).stream().toList());
    }

    // ====================== INTERNALS ======================

    private static String listKey(String statusQuery) {
        if (statusQuery == null || statusQuery.isBlank()) {
            return LIST_KEY_PREFIX + "all";
        }
        return LIST_KEY_PREFIX + statusQuery.toUpperCase();
    }

    private static String detailKey(String marketId) {
        return DETAIL_KEY_PREFIX + marketId;
    }

    private <T> T readValue(String key, TypeReference<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) return null;
            return objectMapper.readValue(json, type);
        } catch (RuntimeException | java.io.IOException e) {
            onError("read", key, e);
            return null;
        }
    }

    private void writeValue(String key, Object value, Duration ttl) {
        if (value == null) return;
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RuntimeException | java.io.IOException e) {
            onError("write", key, e);
        }
    }

    private void deleteKeys(List<String> keys) {
        try {
            redis.delete(keys);
        } catch (RuntimeException e) {
            onError("delete", String.join(",", keys), e);
        }
    }

    private void onError(String op, String key, Exception e) {
        long now = System.currentTimeMillis();
        long next = nextWarnLogMillis.get();
        if (now >= next && nextWarnLogMillis.compareAndSet(next, now + WARN_THROTTLE_MILLIS)) {
            log.warn("Redis cache {} failed for key [{}]: {}", op, key, e.toString());
        }
    }
}
