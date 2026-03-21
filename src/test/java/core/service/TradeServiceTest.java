package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
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
import core.user.User;
import core.service.UserService;

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

    private TradeService tradeService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tradeService = new TradeService(tradeRepository, userService, marketRepository, 
                tradeEngine, marketStore, eventPublisher);
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
        List<Trade> actualTrades = tradeService.getTradesByMarketId(marketId);

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
}
