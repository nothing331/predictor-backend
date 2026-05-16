package core.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.settlement.SettlementEngine;
import core.store.MarketStore;
import core.user.User;

public class MarketServiceTest {

    @Mock
    private MarketRepository repository;
    @Mock
    private MarketStore marketStore;
    @Mock
    private SettlementEngine settlementEngine;
    @Mock
    private UserService userService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private TradeService tradeService;
    @Mock
    private LedgerService ledgerService;

    private MarketService marketService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        marketService = new MarketService(repository, marketStore, settlementEngine, userService, eventPublisher,
                tradeService, ledgerService);
    }

    @Test
    public void testResolveMarket_OrchestratesSettlementAndPersistence() {
        // Arrange
        String marketId = "market-1";
        Market market = new Market(marketId, "Test Market", "Desc");

        when(marketStore.get(marketId)).thenReturn(market);

        User user1 = new User("u1");
        List<User> users = Arrays.asList(user1);

        when(userService.loadAll()).thenReturn(users);
        when(marketStore.getAll()).thenReturn(Arrays.asList(market));

        // Act
        marketService.resolveMarket(marketId, "YES");

        // Assert
        // verify state changes
        assertEquals(MarketStatus.RESOLVED, market.getStatus());
        assertEquals(Outcome.YES, market.getResolvedOutcome());

        // verify interactions
        verify(settlementEngine, never()).settleMarket(any(), any());
        verify(userService).saveAll(List.of());
        verify(repository).saveAll(any());
    }

    @Test
    public void testGetMarketById_resolvedMarket_emitsCertainties() {
        // Arrange
        String marketId = "market-resolved-1";
        Market market = new Market(marketId, "Resolved Market", "Desc");
        // Simulate LMSR price shift (not 1.0 / 0.0)
        market.applyTrade(Outcome.NO, 50); 
        market.resolveMarket(Outcome.NO);

        when(marketStore.get(marketId)).thenReturn(market);
        when(tradeService.getTotalCostByMarketId(marketId)).thenReturn(new java.math.BigDecimal("150.00"));

        // Act
        api.dto.GetAllMarket response = marketService.getMarketById(marketId);

        // Assert
        assertEquals(Outcome.NO, response.getResolvedOutcome());
        assertEquals(MarketStatus.RESOLVED, response.getStatus());

        // Even though LMSR prices are something like 0.3 / 0.7, 
        // the response should emit the resolved certainty (1.0 for NO, 0.0 for YES).
        for (core.market.MarketStats stats : response.getOutcomes()) {
            if (stats.getOutcomeId() == Outcome.NO) {
                assertEquals(1.0, stats.getProbability(), 0.001);
            } else if (stats.getOutcomeId() == Outcome.YES) {
                assertEquals(0.0, stats.getProbability(), 0.001);
            }
        }
        
        assertEquals(new java.math.BigDecimal("150.00"), response.getTotalValue());
    }
}
