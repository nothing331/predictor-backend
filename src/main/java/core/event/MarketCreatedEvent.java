package core.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MarketCreatedEvent(
        String eventId,
        String type,
        Instant occurredAt,
        String marketId,
        Map<String, Object> payload) implements DomainEvent {

    public MarketCreatedEvent(String marketId, String marketName) {
        this(
                UUID.randomUUID().toString(),
                "MarketCreated",
                Instant.now(),
                marketId,
                Map.of("marketName", marketName));
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
