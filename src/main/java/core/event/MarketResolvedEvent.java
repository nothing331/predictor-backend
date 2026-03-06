package core.event;

public record MarketResolvedEvent(String marketId, String outcomeId) implements DomainEvent {
    @Override
    public String getEventType() {
        return "MarketResolved";
    }
}
