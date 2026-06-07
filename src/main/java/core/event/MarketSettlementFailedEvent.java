package core.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Published when a Market transitions {@code RESOLUTION_PENDING -> SETTLEMENT_FAILED}
 * (a Position settlement row exhausted its retries or hit a terminal failure).
 * Broadcast over SSE as {@code MarketSettlementFailed} and consumed by the Redis
 * cache invalidator. Emitted once per Market, only on the actual status flip.
 */
public record MarketSettlementFailedEvent(
        String eventId,
        String type,
        Instant occurredAt,
        String marketId,
        Map<String, Object> payload) implements DomainEvent {

    public MarketSettlementFailedEvent(String marketId) {
        this(
                UUID.randomUUID().toString(),
                "MarketSettlementFailed",
                Instant.now(),
                marketId,
                Map.of());
    }

    @Override
    public Map<String, Object> getEvent() {
        return Map.of(
                "eventId", eventId,
                "type", type,
                "occurredAt", occurredAt.toString(),
                "marketId", marketId,
                "payload", payload);
    }
}
