package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import api.dto.BuyRequest;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.repository.port.TradeRepository;
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
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private LedgerService ledgerService;

    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tradeService = new TradeService(tradeRepository, userService, marketRepository,
                tradeEngine, eventPublisher, ledgerService);
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

    @Test
    @DisplayName("buy generates a clientRequestId when the request omits one")
    void buyGeneratesClientRequestIdWhenMissing() {
        BuyRequest request = new BuyRequest();
        request.setOutcome("YES");
        request.setAmount(200.0);
        // clientRequestId intentionally left unset (null)

        when(tradeRepository.loadByUserIdAndClientRequestId(eq("user-1"), anyString()))
                .thenReturn(java.util.Optional.empty());
        // marketRepository.loadByIdForShare returns null (default) -> "Market not found"

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tradeService.buy(request, "user-1", "market-1"));

        // Reaching the market lookup proves the blank-clientRequestId guard no longer rejects the request.
        assertEquals("Market not found: market-1", ex.getMessage());

        // A non-blank id was generated server-side and used for the idempotency lookup.
        org.mockito.ArgumentCaptor<String> idCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(tradeRepository).loadByUserIdAndClientRequestId(eq("user-1"), idCaptor.capture());
        assertFalse(idCaptor.getValue() == null || idCaptor.getValue().isBlank());
        assertFalse(request.getClientRequestId() == null || request.getClientRequestId().isBlank());
    }
}
