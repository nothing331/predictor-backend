package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import api.dto.UserMarketPositionResponse;
import core.market.Market;
import core.market.Outcome;
import core.store.MarketStore;
import core.trade.Trade;
import core.user.User;

public class MarketUserPositionServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private TradeService tradeService;

    @Mock
    private MarketStore marketStore;

    private MarketUserPositionService marketUserPositionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        marketUserPositionService = new MarketUserPositionService(userService, tradeService, marketStore);
    }

    @Test
    void returnsEmptyStateForExistingMarketWithNoTrades() {
        User user = new User("user-1");
        Market market = new Market("market-1", "Market 1", "Desc");

        when(userService.getUserById("user-1")).thenReturn(user);
        when(marketStore.get("market-1")).thenReturn(market);
        when(tradeService.getTradesByUserIdAndMarketIdOrderedDesc("user-1", "market-1")).thenReturn(List.of());

        UserMarketPositionResponse response = marketUserPositionService.getMarketPosition("user-1", "market-1");

        assertEquals("user-1", response.userId());
        assertEquals("market-1", response.marketId());
        assertEquals(0.0, response.yesSharesHeld(), 0.0001);
        assertEquals(0.0, response.noSharesHeld(), 0.0001);
        assertEquals(BigDecimal.ZERO, response.totalInvested());
        assertEquals(0, response.tradeCount());
        assertEquals(List.of(), response.trades());
        assertNull(response.firstTradeAt());
        assertNull(response.lastTradeAt());
        assertNull(response.realizedPayout());
        assertNull(response.realizedNetPnl());
    }

    @Test
    void aggregatesOpenMarketTradesAndLeavesRealizedFieldsNull() {
        User user = new User("user-1");
        Market market = new Market("market-1", "Market 1", "Desc");
        market.applyTrade(Outcome.YES, 20.0);
        market.applyTrade(Outcome.NO, 5.0);

        List<Trade> trades = List.of(
                trade("t2", "user-1", "market-1", Outcome.NO, 1.5, "3.00", "2026-03-27T12:00:00Z"),
                trade("t1", "user-1", "market-1", Outcome.YES, 4.0, "6.50", "2026-03-27T10:00:00Z"));

        when(userService.getUserById("user-1")).thenReturn(user);
        when(marketStore.get("market-1")).thenReturn(market);
        when(tradeService.getTradesByUserIdAndMarketIdOrderedDesc("user-1", "market-1")).thenReturn(trades);

        UserMarketPositionResponse response = marketUserPositionService.getMarketPosition("user-1", "market-1");

        assertEquals(4.0, response.yesSharesHeld(), 0.0001);
        assertEquals(1.5, response.noSharesHeld(), 0.0001);
        assertEquals(new BigDecimal("9.50"), response.totalInvested());
        assertEquals(new BigDecimal("6.50"), response.totalYesInvested());
        assertEquals(new BigDecimal("3.00"), response.totalNoInvested());
        assertEquals(Instant.parse("2026-03-27T10:00:00Z"), response.firstTradeAt());
        assertEquals(Instant.parse("2026-03-27T12:00:00Z"), response.lastTradeAt());
        assertEquals(new BigDecimal("4.0"), response.projectedPayoutIfYes());
        assertEquals(new BigDecimal("1.5"), response.projectedPayoutIfNo());
        assertNull(response.realizedPayout());
        assertNull(response.realizedNetPnl());
        assertEquals(market.getYesPrice(), response.currentYesChance(), 0.0001);
        assertEquals(market.getNoPrice(), response.currentNoChance(), 0.0001);
        assertEquals(2, response.tradeCount());
        assertEquals("t2", response.trades().get(0).tradeId());
    }

    @Test
    void computesResolvedPayoutAndNetPnlFromWinningSideOnly() {
        User user = new User("user-1");
        Market market = new Market("market-1", "Market 1", "Desc");
        market.resolveMarket(Outcome.NO);

        List<Trade> trades = List.of(
                trade("t3", "user-1", "market-1", Outcome.NO, 3.0, "4.50", "2026-03-27T12:00:00Z"),
                trade("t2", "user-1", "market-1", Outcome.YES, 2.0, "5.00", "2026-03-27T11:00:00Z"),
                trade("t1", "user-1", "market-1", Outcome.NO, 1.5, "2.00", "2026-03-27T10:00:00Z"));

        when(userService.getUserById("user-1")).thenReturn(user);
        when(marketStore.get("market-1")).thenReturn(market);
        when(tradeService.getTradesByUserIdAndMarketIdOrderedDesc("user-1", "market-1")).thenReturn(trades);

        UserMarketPositionResponse response = marketUserPositionService.getMarketPosition("user-1", "market-1");

        assertEquals(2.0, response.yesSharesHeld(), 0.0001);
        assertEquals(4.5, response.noSharesHeld(), 0.0001);
        assertEquals(new BigDecimal("11.50"), response.totalInvested());
        assertEquals(new BigDecimal("4.5"), response.realizedPayout());
        assertEquals(new BigDecimal("-7.00"), response.realizedNetPnl());
        assertEquals(0.0, response.currentYesChance(), 0.0001);
        assertEquals(1.0, response.currentNoChance(), 0.0001);
    }

    @Test
    void returnsNotFoundForMissingMarket() {
        when(userService.getUserById("user-1")).thenReturn(new User("user-1"));
        when(marketStore.get("missing-market")).thenReturn(null);

        try {
            marketUserPositionService.getMarketPosition("user-1", "missing-market");
        } catch (ResponseStatusException ex) {
            assertEquals(404, ex.getStatusCode().value());
            return;
        }

        throw new AssertionError("Expected ResponseStatusException");
    }

    private Trade trade(String tradeId, String userId, String marketId, Outcome outcome, double shares, String cost,
            String timestamp) {
        return new Trade(
                tradeId,
                userId,
                marketId,
                outcome,
                shares,
                new BigDecimal(cost),
                Instant.parse(timestamp));
    }
}
