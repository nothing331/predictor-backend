package core.trade;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import core.market.Market;
import core.market.Outcome;
import core.user.Position;
import core.user.User;

/**
 * TradeEngine Correctness Tests - Week 2 Mandatory Tests
 * 
 * These tests are the contract. They verify:
 * 1. Insufficient balance prevents trades
 * 2. Balance deductions are exact (no rounding errors)
 * 3. Position updates are correct
 * 4. Market share updates maintain LMSR integrity
 * 5. Failed trades have ZERO side effects (atomicity guarantee)
 * 
 * If all 5 tests pass, the engine is trustworthy.
 */
public class TradeEngineTest {

    private TradeEngine tradeEngine;
    private Market market;
    private User user;

    @BeforeEach
    public void setUp() {
        tradeEngine = new TradeEngine();
        market = new Market("market-1", "Will it rain tomorrow?", "Prediction market for weather");
        user = new User("user-1", new BigDecimal("1000.00"));
    }

    // ========================================================================
    // TEST 1: INSUFFICIENT BALANCE TEST
    // Purpose: Prevent free money
    // ========================================================================

    @Test
    public void testInsufficientBalance_TradeFailsAndStateUnchanged() {
        // ARRANGE
        // Give user exactly 10.00, then try to trade for 10.01
        BigDecimal initialBalance = new BigDecimal("10.00");
        user = new User("user-insufficient", initialBalance);

        // Calculate how many shares would cost slightly more than balance
        // We'll find the cost first, then reduce balance to be just below it
        BigDecimal tradeCost = market.getCostToBuy(Outcome.YES, 10);

        // Set balance to be 0.01 less than required
        BigDecimal insufficientBalance = tradeCost.subtract(new BigDecimal("0.01"));
        user = new User("user-insufficient", insufficientBalance);

        // Capture initial state
        BigDecimal balanceBefore = user.getBalance();
        double qYesBefore = market.getQYes();
        double qNoBefore = market.getQNo();
        int positionsCountBefore = user.getPositions().size();

        // ACT & ASSERT
        // Trade should fail with InsufficientBalanceException
        assertThrows(TradeEngine.InsufficientBalanceException.class, () -> {
            tradeEngine.executeTrade(user, market, Outcome.YES, 10);
        });

        // ASSERT: No state changes occurred
        assertEquals(balanceBefore, user.getBalance(), "Balance should be unchanged after failed trade");
        assertEquals(qYesBefore, market.getQYes(), 0.0001, "Market qYes should be unchanged");
        assertEquals(qNoBefore, market.getQNo(), 0.0001, "Market qNo should be unchanged");
        assertEquals(positionsCountBefore, user.getPositions().size(), "No new position should be created");
    }

    // ========================================================================
    // TEST 2: EXACT BALANCE DEDUCTION TEST
    // Purpose: Catch rounding or precision errors
    // 🚨 Never use doubles for balances
    // ========================================================================

    @Test
    public void testExactBalanceDeduction_NoPrecisionLoss() {
        // ARRANGE
        BigDecimal initialBalance = new BigDecimal("500.00");
        user = new User("user-exact", initialBalance);

        // ACT
        Trade trade = tradeEngine.executeTrade(user, market, Outcome.YES, 5);
        BigDecimal tradeCost = trade.getCost();
        BigDecimal finalBalance = user.getBalance();

        // ASSERT
        // initialBalance - cost = finalBalance (exact BigDecimal arithmetic)
        BigDecimal expectedBalance = initialBalance.subtract(tradeCost);

        assertEquals(expectedBalance, finalBalance,
                "Balance deduction must be exact: initialBalance - cost = finalBalance");

        // Verify it's using BigDecimal precision (not double approximation)
        // The .equals() method checks both value and scale
        assertTrue(expectedBalance.compareTo(finalBalance) == 0,
                "Balance calculation must use BigDecimal precision");
    }

    // ========================================================================
    // TEST 3: POSITION UPDATE TEST
    // Purpose: Ownership correctness
    // ========================================================================

    @Test
    public void testPositionUpdate_OwnershipCorrectness() {
        // ARRANGE
        String marketId = market.getMarketId();
        int sharesToBuy = 10;

        // ASSERT: No position exists initially
        assertNull(user.getPosition(marketId), "No position should exist before first trade");

        // ACT: Buy YES shares
        tradeEngine.executeTrade(user, market, Outcome.YES, sharesToBuy);

        // ASSERT: Position created and updated correctly
        Position position = user.getPosition(marketId);
        assertNotNull(position, "Position should exist after trade");
        assertEquals(marketId, position.getMarketId(), "Position should be for correct market");
        assertEquals(sharesToBuy, position.getYesShares(), 0.0001, "YES shares should increase by amount bought");
        assertEquals(0.0, position.getNoShares(), 0.0001, "NO shares should remain unchanged");

        // ACT: Buy more YES shares
        int additionalShares = 5;
        tradeEngine.executeTrade(user, market, Outcome.YES, additionalShares);

        // ASSERT: Position accumulated correctly
        position = user.getPosition(marketId);
        assertEquals(sharesToBuy + additionalShares, position.getYesShares(), 0.0001,
                "YES shares should accumulate across multiple trades");
        assertEquals(0.0, position.getNoShares(), 0.0001, "NO shares should still be unchanged");

        // ACT: Buy NO shares
        int noShares = 3;
        tradeEngine.executeTrade(user, market, Outcome.NO, noShares);

        // ASSERT: Both positions updated independently
        position = user.getPosition(marketId);
        assertEquals(sharesToBuy + additionalShares, position.getYesShares(), 0.0001,
                "YES shares should be unchanged after NO trade");
        assertEquals(noShares, position.getNoShares(), 0.0001,
                "NO shares should increase by amount bought");
    }

    // ========================================================================
    // TEST 4: MARKET SHARE UPDATE TEST
    // Purpose: LMSR integrity
    // ========================================================================

    @Test
    public void testMarketShareUpdate_LMSRIntegrity() {
        // ARRANGE
        double qYesBefore = market.getQYes();
        double qNoBefore = market.getQNo();
        double yesPriceBefore = market.getYesPrice();
        double noPriceBefore = market.getNoPrice();
        int yesSharesToBuy = 20;

        // ACT: Buy YES shares
        tradeEngine.executeTrade(user, market, Outcome.YES, yesSharesToBuy);

        // ASSERT: Market state updated correctly
        double qYesAfter = market.getQYes();
        double qNoAfter = market.getQNo();
        double yesPriceAfter = market.getYesPrice();
        double noPriceAfter = market.getNoPrice();

        // qYes should increase by exact shares bought
        assertEquals(qYesBefore + yesSharesToBuy, qYesAfter, 0.0001,
                "qYes should increase by shares bought");

        // qNo should be completely unchanged
        assertEquals(qNoBefore, qNoAfter, 0.0001,
                "qNo should be unchanged when buying YES");

        // LMSR property: buying YES increases YES price, decreases NO price
        assertTrue(yesPriceAfter > yesPriceBefore,
                "YES price should increase after buying YES shares");
        assertTrue(noPriceAfter < noPriceBefore,
                "NO price should decrease after buying YES shares");

        // Prices must still sum to 1 (LMSR invariant)
        assertEquals(1.0, yesPriceAfter + noPriceAfter, 0.001,
                "Prices must sum to 1 (LMSR invariant)");

        // Prices must be bounded [0, 1]
        assertTrue(yesPriceAfter > 0.0 && yesPriceAfter < 1.0,
                "YES price must be between 0 and 1");
        assertTrue(noPriceAfter > 0.0 && noPriceAfter < 1.0,
                "NO price must be between 0 and 1");
    }

    // ========================================================================
    // TEST 5: FAILED TRADE HAS ZERO SIDE EFFECTS
    // Most important test - validates atomicity
    // ========================================================================

    @Test
    public void testFailedTrade_ZeroSideEffects() {
        // ARRANGE
        // Set up user with insufficient balance
        BigDecimal insufficientBalance = new BigDecimal("1.00");
        user = new User("user-atomic", insufficientBalance);

        // Capture complete initial state
        BigDecimal balanceBefore = user.getBalance();
        double qYesBefore = market.getQYes();
        double qNoBefore = market.getQNo();
        double yesPriceBefore = market.getYesPrice();
        double noPriceBefore = market.getNoPrice();
        int positionsCountBefore = user.getPositions().size();

        // ACT: Force failure with insufficient funds
        assertThrows(TradeEngine.InsufficientBalanceException.class, () -> {
            tradeEngine.executeTrade(user, market, Outcome.YES, 100);
        });

        // ASSERT: Absolutely ZERO state changes

        // 1. User balance unchanged
        assertEquals(balanceBefore, user.getBalance(),
                "User balance must be unchanged after failed trade");

        // 2. Market shares unchanged
        assertEquals(qYesBefore, market.getQYes(), 0.0001,
                "Market qYes must be unchanged after failed trade");
        assertEquals(qNoBefore, market.getQNo(), 0.0001,
                "Market qNo must be unchanged after failed trade");

        // 3. Market prices unchanged (derived from shares)
        assertEquals(yesPriceBefore, market.getYesPrice(), 0.0001,
                "YES price must be unchanged after failed trade");
        assertEquals(noPriceBefore, market.getNoPrice(), 0.0001,
                "NO price must be unchanged after failed trade");

        // 4. No position created
        assertEquals(positionsCountBefore, user.getPositions().size(),
                "No new position should be created after failed trade");
        assertNull(user.getPosition(market.getMarketId()),
                "Position should not exist after failed trade");

        // CRITICAL: If this test passes, your engine is trustworthy
        // It proves that validation happens BEFORE any mutations
        // This is the atomicity guarantee
    }

    // ========================================================================
    // BONUS: Test multiple failure scenarios for atomicity
    // ========================================================================

    @Test
    public void testAtomicity_VariousFailureScenarios() {
        // Test 1: Invalid shares (negative)
        BigDecimal balanceBefore = user.getBalance();
        double qYesBefore = market.getQYes();

        assertThrows(IllegalArgumentException.class, () -> {
            tradeEngine.executeTrade(user, market, Outcome.YES, -5);
        });

        assertEquals(balanceBefore, user.getBalance());
        assertEquals(qYesBefore, market.getQYes(), 0.0001);

        // Test 2: Invalid shares (zero)
        assertThrows(IllegalArgumentException.class, () -> {
            tradeEngine.executeTrade(user, market, Outcome.YES, 0);
        });

        assertEquals(balanceBefore, user.getBalance());
        assertEquals(qYesBefore, market.getQYes(), 0.0001);

        // Test 3: Closed market
        market.resolveMarket(Outcome.YES);

        assertThrows(IllegalStateException.class, () -> {
            tradeEngine.executeTrade(user, market, Outcome.YES, 10);
        });

        assertEquals(balanceBefore, user.getBalance());
        // qYes unchanged since market is resolved
    }
}
