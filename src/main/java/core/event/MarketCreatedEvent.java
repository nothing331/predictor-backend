package core.event;

import java.time.Instant;
import java.util.LinkedHashMap;
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

    public MarketCreatedEvent(String marketId, String marketName, String actorUserId) {
        this(
                UUID.randomUUID().toString(),
                "MarketCreated",
                Instant.now(),
                marketId,
                createPayload(marketName, actorUserId));
    }

    private static Map<String, Object> createPayload(String marketName, String actorUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("marketName", marketName);
        if (actorUserId != null && !actorUserId.isBlank()) {
            payload.put("actorUserId", actorUserId);
        }
        return Map.copyOf(payload);
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
