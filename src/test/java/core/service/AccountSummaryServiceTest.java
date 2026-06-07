package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import api.dto.UserAccountSummaryResponse;
import api.dto.UserRecentMarketSummary;
import core.market.Market;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.trade.Trade;
import core.user.User;

public class AccountSummaryServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private TradeService tradeService;

    @Mock
    private MarketRepository marketRepository;

    private AccountSummaryService accountSummaryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accountSummaryService = new AccountSummaryService(userService, tradeService, marketRepository);
    }

    @Test
    void getSummaryReturnsBalanceAndEmptyRecentMarketsWhenUserHasNoTrades() {
        User user = new User("user-1");
        user.setBalance(new BigDecimal("850.50"));

        when(userService.getUserById("user-1")).thenReturn(user);
        when(tradeService.getTradesByUserIdOrderedDesc("user-1")).thenReturn(List.of());

        UserAccountSummaryResponse response = accountSummaryService.getSummary("user-1");

        assertEquals("user-1", response.userId());
        assertEquals(new BigDecimal("850.50"), response.availableBalance());
        assertTrue(response.recentMarkets().isEmpty());
    }

    @Test
    void getSummaryAggregatesRecentUniqueMarketsAndOrdersByLatestTrade() {
        User user = new User("user-1");
        when(userService.getUserById("user-1")).thenReturn(user);

        List<Trade> trades = List.of(
                trade("t1", "user-1", "market-c", Outcome.YES, 3.0, "2026-03-27T12:00:00Z"),
                trade("t2", "user-1", "market-a", Outcome.YES, 2.0, "2026-03-27T11:00:00Z"),
                trade("t3", "user-1", "market-c", Outcome.NO, 1.5, "2026-03-27T10:30:00Z"),
                trade("t4", "user-1", "market-b", Outcome.NO, 4.0, "2026-03-27T10:00:00Z"),
                trade("t5", "user-1", "market-d", Outcome.YES, 5.0, "2026-03-27T09:00:00Z"),
                trade("t6", "user-1", "market-a", Outcome.YES, 1.0, "2026-03-27T08:00:00Z"));

        Market marketA = new Market("market-a", "Market A", "Desc");
        marketA.applyTrade(Outcome.YES, 20.0);
        Market marketB = new Market("market-b", "Market B", "Desc");
        marketB.resolveMarket(Outcome.NO);
        Market marketC = new Market("market-c", "Market C", "Desc");
        marketC.applyTrade(Outcome.NO, 15.0);

        when(tradeService.getTradesByUserIdOrderedDesc("user-1")).thenReturn(trades);
        when(marketRepository.loadById("market-a")).thenReturn(marketA);
        when(marketRepository.loadById("market-b")).thenReturn(marketB);
        when(marketRepository.loadById("market-c")).thenReturn(marketC);

        UserAccountSummaryResponse response = accountSummaryService.getSummary("user-1");

        assertEquals(3, response.recentMarkets().size());

        UserRecentMarketSummary first = response.recentMarkets().get(0);
        assertEquals("market-c", first.marketId());
        assertEquals(3.0, first.userYesShares(), 0.0001);
        assertEquals(1.5, first.userNoShares(), 0.0001);
        assertEquals(Instant.parse("2026-03-27T12:00:00Z"), first.lastTradedAt());

        UserRecentMarketSummary second = response.recentMarkets().get(1);
        assertEquals("market-a", second.marketId());
        assertEquals(3.0, second.userYesShares(), 0.0001);
        assertEquals(0.0, second.userNoShares(), 0.0001);

        UserRecentMarketSummary third = response.recentMarkets().get(2);
        assertEquals("market-b", third.marketId());
        assertEquals(0.0, third.userYesShares(), 0.0001);
        assertEquals(4.0, third.userNoShares(), 0.0001);
    }

    @Test
    void getSummaryUsesLiveOddsForOpenMarketsAndSettledOddsForResolvedMarkets() {
        User user = new User("user-1");
        when(userService.getUserById("user-1")).thenReturn(user);

        List<Trade> trades = List.of(
                trade("t1", "user-1", "market-open", Outcome.YES, 2.0, "2026-03-27T12:00:00Z"),
                trade("t2", "user-1", "market-resolved", Outcome.NO, 5.0, "2026-03-27T11:00:00Z"));

        Market openMarket = new Market("market-open", "Open Market", "Desc");
        openMarket.applyTrade(Outcome.YES, 25.0);
        openMarket.applyTrade(Outcome.NO, 5.0);

        Market resolvedMarket = new Market("market-resolved", "Resolved Market", "Desc");
        resolvedMarket.resolveMarket(Outcome.YES);

        when(tradeService.getTradesByUserIdOrderedDesc("user-1")).thenReturn(trades);
        when(marketRepository.loadById("market-open")).thenReturn(openMarket);
        when(marketRepository.loadById("market-resolved")).thenReturn(resolvedMarket);

        UserAccountSummaryResponse response = accountSummaryService.getSummary("user-1");

        UserRecentMarketSummary openSummary = response.recentMarkets().get(0);
        assertEquals(openMarket.getYesPrice(), openSummary.currentYesChance(), 0.0001);
        assertEquals(openMarket.getNoPrice(), openSummary.currentNoChance(), 0.0001);

        UserRecentMarketSummary resolvedSummary = response.recentMarkets().get(1);
        assertEquals(1.0, resolvedSummary.currentYesChance(), 0.0001);
        assertEquals(0.0, resolvedSummary.currentNoChance(), 0.0001);
    }

    @Test
    void getSummaryUsesTradeHistoryForProjectedPayouts() {
        User user = new User("user-1");
        when(userService.getUserById("user-1")).thenReturn(user);

        List<Trade> trades = List.of(
                trade("t1", "user-1", "market-1", Outcome.YES, 7.5, "2026-03-27T12:00:00Z"),
                trade("t2", "user-1", "market-1", Outcome.NO, 1.25, "2026-03-27T11:00:00Z"));

        Market market = new Market("market-1", "Market 1", "Desc");

        when(tradeService.getTradesByUserIdOrderedDesc("user-1")).thenReturn(trades);
        when(marketRepository.loadById("market-1")).thenReturn(market);

        UserAccountSummaryResponse response = accountSummaryService.getSummary("user-1");
        UserRecentMarketSummary summary = response.recentMarkets().get(0);

        assertEquals(new BigDecimal("7.5"), summary.projectedPayoutIfYes());
        assertEquals(new BigDecimal("1.25"), summary.projectedPayoutIfNo());
    }

    @Test
    void getSummaryReturnsNotFoundWhenUserDoesNotExist() {
        when(userService.getUserById("missing")).thenReturn(null);

        try {
            accountSummaryService.getSummary("missing");
        } catch (ResponseStatusException ex) {
            assertEquals(404, ex.getStatusCode().value());
            return;
        }
        throw new AssertionError("Expected ResponseStatusException");
    }

    private Trade trade(String tradeId, String userId, String marketId, Outcome outcome, double shares,
            String timestamp) {
        return new Trade(
                tradeId,
                userId,
                marketId,
                outcome,
                shares,
                BigDecimal.ONE,
                Instant.parse(timestamp));
    }
}
