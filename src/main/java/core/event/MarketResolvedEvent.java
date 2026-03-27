package core.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record MarketResolvedEvent(
        String eventId,
        String type,
        Instant occurredAt,
        String marketId,
        Map<String, Object> payload) implements DomainEvent {

    public MarketResolvedEvent(String marketId, String outcomeId) {
        this(
                UUID.randomUUID().toString(),
                "MarketResolved",
                Instant.now(),
                marketId,
                Map.of("outcomeId", outcomeId));
    }

    public MarketResolvedEvent(String marketId, String outcomeId, String actorUserId) {
        this(
                UUID.randomUUID().toString(),
                "MarketResolved",
                Instant.now(),
                marketId,
                createPayload(outcomeId, actorUserId));
    }

    private static Map<String, Object> createPayload(String outcomeId, String actorUserId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outcomeId", outcomeId);
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
