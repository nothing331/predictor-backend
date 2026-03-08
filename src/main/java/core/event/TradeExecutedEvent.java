package core.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TradeExecutedEvent(
        String eventId,
        String type,
        Instant occurredAt,
        String marketId,
        Map<String, Object> payload) implements DomainEvent {

    public TradeExecutedEvent(String tradeId, String marketId, String userId, String outcome, double shareCount,
            BigDecimal cost) {
        this(
                UUID.randomUUID().toString(),
                "TradeExecuted",
                Instant.now(),
                marketId,
                Map.of(
                        "tradeId", tradeId,
                        "userId", userId,
                        "outcome", outcome,
                        "shareCount", shareCount,
                        "cost", cost));
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
