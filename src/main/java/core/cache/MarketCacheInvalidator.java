package core.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import core.event.MarketCreatedEvent;
import core.event.MarketResolvedEvent;
import core.event.MarketSettlementCompletedEvent;
import core.event.TradeExecutedEvent;

/**
 * Invalidates Market cache entries after the writing transaction commits.
 *
 * <p>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} means the cache
 * is only touched once the new DB state is durable. A failed cache delete
 * leaves the cache stale at most until its TTL expires (3s list / 2s detail).
 *
 * <p>Per-event rules (see ADR-0005 / Q8c):
 * <ul>
 *   <li>{@link TradeExecutedEvent} → invalidate detail + all lists.</li>
 *   <li>{@link MarketCreatedEvent} → invalidate all lists (no detail yet
 *       cached for a brand-new market).</li>
 *   <li>{@link MarketResolvedEvent} → invalidate detail + all lists. Fires on
 *       the {@code OPEN -> RESOLUTION_PENDING} transition.</li>
 *   <li>{@link MarketSettlementCompletedEvent} → invalidate detail + all lists.
 *       Fires on the {@code RESOLUTION_PENDING -> RESOLVED} flip.</li>
 * </ul>
 *
 * <p>No invalidation for per-Position settlement — the cached DTOs don't depend
 * on per-Position state, so settling one user's payout doesn't change what
 * other users see.
 */
@Component
@ConditionalOnProperty(name = "spring.data.redis.url")
public class MarketCacheInvalidator {

    private final MarketReadCache cache;

    public MarketCacheInvalidator(MarketReadCache cache) {
        this.cache = cache;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTradeExecuted(TradeExecutedEvent event) {
        cache.invalidateMarket(event.marketId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketCreated(MarketCreatedEvent event) {
        cache.invalidateAllLists();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketResolved(MarketResolvedEvent event) {
        cache.invalidateMarket(event.marketId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketSettlementCompleted(MarketSettlementCompletedEvent event) {
        cache.invalidateMarket(event.marketId());
    }
}
