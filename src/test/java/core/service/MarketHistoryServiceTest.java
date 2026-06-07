package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import api.dto.MarketHistoryResponse;
import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.trade.Trade;

public class MarketHistoryServiceTest {

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private TradeService tradeService;

    private MarketHistoryService marketHistoryService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        marketHistoryService = new MarketHistoryService(marketRepository, tradeService);
    }

    @Test
    public void getHistory_returnsInitialPointForNewMarket() {
        Market market = new Market("market-1", "Will BTC Rise?", "Desc");
        when(marketRepository.loadById("market-1")).thenReturn(market);
        when(tradeService.getTradesByMarketIdOrdered("market-1")).thenReturn(List.of());

        MarketHistoryResponse response = marketHistoryService.getHistory("market-1", null, null, 200);

        assertEquals("market-1", response.getMarketId());
        assertEquals(MarketStatus.OPEN, response.getStatus());
        assertEquals(1, response.getPoints().size());
        assertEquals("INITIAL", response.getPoints().get(0).getEventType());
        assertEquals(0.5, response.getPoints().get(0).getYesProbability(), 0.0001);
        assertEquals(0.5, response.getPoints().get(0).getNoProbability(), 0.0001);
    }

    @Test
    public void getHistory_replaysTradesAndResolution() {
        Market market = new Market("market-2", "Will ETH Rise?", "Desc");
        Instant createdAt = Instant.parse("2026-03-22T10:00:00Z");
        setMarketTimestamp(market, "createdAt", createdAt);
        setMarketTimestamp(market, "resolvedAt", Instant.parse("2026-03-22T10:15:00Z"));
        setMarketField(market, "status", MarketStatus.RESOLVED);
        setMarketField(market, "resolvedOutcome", Outcome.YES);

        Trade trade1 = new Trade("t1", "u1", "market-2", Outcome.YES, 10.0, new BigDecimal("5.00"),
                Instant.parse("2026-03-22T10:05:00Z"));
        Trade trade2 = new Trade("t2", "u2", "market-2", Outcome.NO, 4.0, new BigDecimal("2.50"),
                Instant.parse("2026-03-22T10:10:00Z"));

        when(marketRepository.loadById("market-2")).thenReturn(market);
        when(tradeService.getTradesByMarketIdOrdered("market-2")).thenReturn(List.of(trade1, trade2));

        MarketHistoryResponse response = marketHistoryService.getHistory("market-2", null, null, 200);

        assertEquals(4, response.getPoints().size());
        assertEquals("INITIAL", response.getPoints().get(0).getEventType());
        assertEquals("TRADE", response.getPoints().get(1).getEventType());
        assertEquals("TRADE", response.getPoints().get(2).getEventType());
        assertEquals("RESOLUTION", response.getPoints().get(3).getEventType());
        assertEquals(1.0, response.getPoints().get(3).getYesProbability(), 0.0001);
        assertEquals(0.0, response.getPoints().get(3).getNoProbability(), 0.0001);
    }

    @Test
    public void getHistory_returnsNotFoundForMissingMarket() {
        when(marketRepository.loadById("missing")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> marketHistoryService.getHistory("missing", null, null, 200));
    }

    private void setMarketTimestamp(Market market, String fieldName, Instant value) {
        setMarketField(market, fieldName, value);
    }

    private void setMarketField(Market market, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = Market.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(market, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
