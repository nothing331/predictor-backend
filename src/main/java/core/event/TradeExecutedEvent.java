package core.event;

import java.math.BigDecimal;

public record TradeExecutedEvent(
        String tradeId,
        String marketId,
        String userId,
        String outcome,
        double shareCount,
        BigDecimal cost) implements DomainEvent {
    @Override
    public String getEventType() {
        return "TradeExecuted";
    }
}
