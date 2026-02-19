package core.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import core.repository.MarketRepository;
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

    private MarketService marketService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        marketService = new MarketService(repository, marketStore, settlementEngine, userService);
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
        verify(settlementEngine).settleMarket(market, users);
        verify(userService).saveAll(users);
        verify(repository).saveAll(any());
    }
}
