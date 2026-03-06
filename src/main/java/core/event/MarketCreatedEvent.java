package core.event;

public record MarketCreatedEvent(String marketId, String marketName) implements DomainEvent {
    @Override
    public String getEventType() {
        return "MarketCreated";
    }
}
