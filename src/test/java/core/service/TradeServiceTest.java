package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.repository.port.TradeRepository;
import core.store.MarketStore;
import core.trade.Trade;
import core.trade.TradeEngine;

public class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private UserService userService;
    @Mock
    private MarketRepository marketRepository;
    @Mock
    private TradeEngine tradeEngine;
    @Mock
    private MarketStore marketStore;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LedgerService ledgerService;

    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tradeService = new TradeService(tradeRepository, userService, marketRepository, 
                tradeEngine, marketStore, eventPublisher, ledgerService);
    }

    @Test
    @DisplayName("getTradesByMarketId delegates to TradeRepository")
    void testGetTradesByMarketId() {
        // Arrange
        String marketId = "test-market-id";
        Trade trade1 = new Trade("user1", marketId, Outcome.YES, 10.0, new BigDecimal("5.00"));
        Trade trade2 = new Trade("user2", marketId, Outcome.NO, 20.0, new BigDecimal("15.00"));
        List<Trade> expectedTrades = Arrays.asList(trade1, trade2);
        
        when(tradeRepository.loadByMarketId(marketId)).thenReturn(expectedTrades);

        // Act
        Collection<Trade> actualTrades = tradeService.getTradesByMarketId(marketId);

        // Assert
        assertEquals(expectedTrades, actualTrades);
        verify(tradeRepository).loadByMarketId(marketId);
    }

    @Test
    @DisplayName("getTotalCostByMarketId delegates to TradeRepository")
    void testGetTotalCostByMarketId() {
        // Arrange
        String marketId = "test-market-id";
        BigDecimal expectedCost = new BigDecimal("42.50");
        
        when(tradeRepository.sumCostByMarketId(marketId)).thenReturn(expectedCost);

        // Act
        BigDecimal actualCost = tradeService.getTotalCostByMarketId(marketId);

        // Assert
        assertEquals(expectedCost, actualCost);
        verify(tradeRepository).sumCostByMarketId(marketId);
    }

    @Test
    @DisplayName("getTradesByUserIdOrderedDesc delegates to TradeRepository")
    void testGetTradesByUserIdOrderedDesc() {
        String userId = "test-user-id";
        Trade trade1 = new Trade("1", userId, "market-1", Outcome.YES, 10.0, new BigDecimal("5.00"),
                java.time.Instant.parse("2026-03-27T10:00:00Z"));
        Trade trade2 = new Trade("2", userId, "market-2", Outcome.NO, 4.0, new BigDecimal("3.00"),
                java.time.Instant.parse("2026-03-27T09:00:00Z"));
        List<Trade> expectedTrades = Arrays.asList(trade1, trade2);

        when(tradeRepository.loadByUserIdOrderedDesc(userId)).thenReturn(expectedTrades);

        Collection<Trade> actualTrades = tradeService.getTradesByUserIdOrderedDesc(userId);

        assertEquals(expectedTrades, actualTrades);
        verify(tradeRepository).loadByUserIdOrderedDesc(userId);
    }

    @Test
    @DisplayName("getTradesByUserIdAndMarketIdOrderedDesc delegates to TradeRepository")
    void testGetTradesByUserIdAndMarketIdOrderedDesc() {
        String userId = "test-user-id";
        String marketId = "test-market-id";
        Trade trade1 = new Trade("1", userId, marketId, Outcome.YES, 10.0, new BigDecimal("5.00"),
                java.time.Instant.parse("2026-03-27T10:00:00Z"));
        Trade trade2 = new Trade("2", userId, marketId, Outcome.NO, 4.0, new BigDecimal("3.00"),
                java.time.Instant.parse("2026-03-27T09:00:00Z"));
        List<Trade> expectedTrades = Arrays.asList(trade1, trade2);

        when(tradeRepository.loadByUserIdAndMarketIdOrderedDesc(userId, marketId)).thenReturn(expectedTrades);

        Collection<Trade> actualTrades = tradeService.getTradesByUserIdAndMarketIdOrderedDesc(userId, marketId);

        assertEquals(expectedTrades, actualTrades);
        verify(tradeRepository).loadByUserIdAndMarketIdOrderedDesc(userId, marketId);
    }
}
