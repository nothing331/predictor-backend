package core.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import core.event.MarketCreatedEvent;
import core.event.MarketResolvedEvent;
import core.event.TradeExecutedEvent;

class DomainEventAnalyticsListenerTest {

    @Mock
    private AnalyticsService analyticsService;

    private DomainEventAnalyticsListener listener;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        listener = new DomainEventAnalyticsListener(analyticsService);
    }

    @Test
    void tradeExecutedEventMapsToBetPlaced() {
        TradeExecutedEvent event = new TradeExecutedEvent(
                "trade-1",
                "market-1",
                "user-1",
                "YES",
                10.0,
                new BigDecimal("12.50"),
                0.55,
                0.45,
                10.0,
                5.0,
                "OPEN");

        ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);

        listener.onTradeExecuted(event);

        verify(analyticsService).capture(eq("user-1"), eq(AnalyticsEventNames.BET_PLACED), propertiesCaptor.capture());
        assertEquals("market-1", propertiesCaptor.getValue().get("marketId"));
        assertEquals("trade-1", propertiesCaptor.getValue().get("tradeId"));
    }

    @Test
    void marketCreatedEventMapsToMarketCreated() {
        ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);

        listener.onMarketCreated(new MarketCreatedEvent("market-1", "Will it rain?", "admin-1"));

        verify(analyticsService).capture(eq("admin-1"), eq(AnalyticsEventNames.MARKET_CREATED),
                propertiesCaptor.capture());
        assertEquals("Will it rain?", propertiesCaptor.getValue().get("marketName"));
    }

    @Test
    void marketResolvedEventWithoutActorIsSkipped() {
        listener.onMarketResolved(new MarketResolvedEvent("market-1", "YES"));

        verifyNoInteractions(analyticsService);
    }
}
