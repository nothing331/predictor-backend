package core.analytics;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import core.event.MarketCreatedEvent;
import core.event.MarketResolvedEvent;
import core.event.TradeExecutedEvent;

@Component
public class DomainEventAnalyticsListener {

    private final AnalyticsService analyticsService;

    public DomainEventAnalyticsListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @EventListener
    public void onTradeExecuted(TradeExecutedEvent event) {
        String userId = asString(event.payload().get("userId"));
        if (!StringUtils.hasText(userId)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>(event.payload());
        properties.put("marketId", event.marketId());

        analyticsService.capture(userId, AnalyticsEventNames.BET_PLACED, properties);
    }

    @EventListener
    public void onMarketCreated(MarketCreatedEvent event) {
        String actorUserId = asString(event.payload().get("actorUserId"));
        if (!StringUtils.hasText(actorUserId)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("marketId", event.marketId());
        properties.put("marketName", event.payload().get("marketName"));

        analyticsService.capture(actorUserId, AnalyticsEventNames.MARKET_CREATED, properties);
    }

    @EventListener
    public void onMarketResolved(MarketResolvedEvent event) {
        String actorUserId = asString(event.payload().get("actorUserId"));
        if (!StringUtils.hasText(actorUserId)) {
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("marketId", event.marketId());
        properties.put("outcomeId", event.payload().get("outcomeId"));

        analyticsService.capture(actorUserId, AnalyticsEventNames.MARKET_RESOLVED, properties);
    }

    private String asString(Object value) {
        return value instanceof String stringValue ? stringValue : null;
    }
}
