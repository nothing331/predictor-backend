package core.event;

/**
 * Published by {@link core.settlement.SettlementBookkeeping#maybeFlipResolved}
 * when a Market transitions {@code RESOLUTION_PENDING -> RESOLVED} (the worker
 * paid out the last Position). Consumed by the Redis cache invalidator and
 * available to analytics / audit listeners.
 *
 * <p>NOT published when the Market lands in {@code SETTLEMENT_FAILED} — that
 * transition has its own observability needs (alerting + admin retry); the
 * "successfully fully settled" signal is intentionally separate.
 */
public record MarketSettlementCompletedEvent(String marketId) {
}
