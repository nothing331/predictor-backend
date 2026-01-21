package core.market;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Market Resolution Tests - Week 3 Mandatory Tests
 * 
 * These tests verify the market resolution behavior:
 * 1. Cannot resolve twice
 * 2. Cannot resolve non-OPEN market (already resolved)
 * 3. Resolved outcome is stored correctly
 * 4. Resolution is deterministic
 */
public class MarketResolutionTest {

    private Market market;

    @BeforeEach
    public void setUp() {
        market = new Market("market-1", "Will it rain tomorrow?", "Weather prediction market");
    }

    // ========================================================================
    // MARKET RESOLUTION TESTS
    // ========================================================================

    @Test
    @DisplayName("Cannot resolve a market twice - throws IllegalStateException")
    public void testCannotResolveTwice() {
        // ARRANGE
        market.resolveMarket(Outcome.YES);

        // ACT & ASSERT
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> market.resolveMarket(Outcome.YES),
                "Should throw IllegalStateException when resolving already resolved market");

        assertTrue(exception.getMessage().contains("already resolved"),
                "Exception message should indicate market is already resolved");
    }

    @Test
    @DisplayName("Cannot resolve twice with different outcomes")
    public void testCannotResolveTwice_DifferentOutcomes() {
        // ARRANGE
        market.resolveMarket(Outcome.YES);

        // ACT & ASSERT: Try to change resolution to NO
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> market.resolveMarket(Outcome.NO),
                "Should throw IllegalStateException when trying to change resolution");

        assertTrue(exception.getMessage().contains("already resolved"),
                "Exception message should indicate market is already resolved");

        // Original resolution should be preserved
        assertEquals(Outcome.YES, market.getResolvedOutcome(),
                "Original resolution should be preserved after failed re-resolution attempt");
        assertEquals(MarketStatus.RESOLVED, market.getStatus(),
                "Market status should remain RESOLVED");
    }

    @Test
    @DisplayName("Cannot resolve non-OPEN market (market already RESOLVED)")
    public void testCannotResolveNonOpenMarket() {
        // ARRANGE: First resolve the market
        market.resolveMarket(Outcome.NO);
        assertEquals(MarketStatus.RESOLVED, market.getStatus());

        // ACT & ASSERT: Cannot resolve again
        assertThrows(
                IllegalStateException.class,
                () -> market.resolveMarket(Outcome.YES),
                "Should not be able to resolve a non-OPEN market");
    }

    @Test
    @DisplayName("Resolved outcome YES is stored correctly")
    public void testResolvedOutcome_YES_StoredCorrectly() {
        // ARRANGE
        assertNull(market.getResolvedOutcome(), "Outcome should be null before resolution");
        assertEquals(MarketStatus.OPEN, market.getStatus(), "Market should be OPEN initially");

        // ACT
        market.resolveMarket(Outcome.YES);

        // ASSERT
        assertEquals(Outcome.YES, market.getResolvedOutcome(),
                "Resolved outcome should be YES");
        assertEquals(MarketStatus.RESOLVED, market.getStatus(),
                "Market status should be RESOLVED after resolution");
    }

    @Test
    @DisplayName("Resolved outcome NO is stored correctly")
    public void testResolvedOutcome_NO_StoredCorrectly() {
        // ARRANGE
        assertNull(market.getResolvedOutcome(), "Outcome should be null before resolution");
        assertEquals(MarketStatus.OPEN, market.getStatus(), "Market should be OPEN initially");

        // ACT
        market.resolveMarket(Outcome.NO);

        // ASSERT
        assertEquals(Outcome.NO, market.getResolvedOutcome(),
                "Resolved outcome should be NO");
        assertEquals(MarketStatus.RESOLVED, market.getStatus(),
                "Market status should be RESOLVED after resolution");
    }

    @Test
    @DisplayName("Resolution with null outcome throws IllegalArgumentException")
    public void testResolveWithNullOutcome_ThrowsException() {
        // ACT & ASSERT
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> market.resolveMarket(null),
                "Should throw IllegalArgumentException for null outcome");

        assertTrue(exception.getMessage().contains("null"),
                "Exception message should mention null");

        // Market state should be unchanged
        assertEquals(MarketStatus.OPEN, market.getStatus(),
                "Market should remain OPEN after failed resolution");
        assertNull(market.getResolvedOutcome(),
                "Outcome should remain null after failed resolution");
    }

    @Test
    @DisplayName("Resolution is deterministic - same input produces same state")
    public void testResolutionIsDeterministic() {
        // ARRANGE: Create two identical markets
        Market market1 = new Market("market-det-1", "Test Market", "Description");
        Market market2 = new Market("market-det-2", "Test Market", "Description");

        // ACT: Resolve both with same outcome
        market1.resolveMarket(Outcome.YES);
        market2.resolveMarket(Outcome.YES);

        // ASSERT: Both markets have identical resolved state
        assertEquals(market1.getResolvedOutcome(), market2.getResolvedOutcome(),
                "Resolved outcomes should be identical");
        assertEquals(market1.getStatus(), market2.getStatus(),
                "Market statuses should be identical");
    }

    @Test
    @DisplayName("Market prices remain valid after resolution")
    public void testMarketPricesValidAfterResolution() {
        // ARRANGE
        double yesPriceBefore = market.getYesPrice();
        double noPriceBefore = market.getNoPrice();

        // Verify initial prices sum to 1
        assertEquals(1.0, yesPriceBefore + noPriceBefore, 0.001,
                "Initial prices should sum to 1");

        // ACT
        market.resolveMarket(Outcome.YES);

        // ASSERT: Prices should still be accessible and valid
        double yesPriceAfter = market.getYesPrice();
        double noPriceAfter = market.getNoPrice();

        // Prices should still sum to 1 (LMSR invariant holds even after resolution)
        assertEquals(1.0, yesPriceAfter + noPriceAfter, 0.001,
                "Prices should still sum to 1 after resolution");

        // Prices should still be bounded
        assertTrue(yesPriceAfter > 0 && yesPriceAfter < 1,
                "YES price should be between 0 and 1");
        assertTrue(noPriceAfter > 0 && noPriceAfter < 1,
                "NO price should be between 0 and 1");
    }

    @Test
    @DisplayName("Market shares are preserved after resolution")
    public void testMarketSharesPreservedAfterResolution() {
        // ARRANGE: Add some shares first
        market.applyTrade(Outcome.YES, 10);
        market.applyTrade(Outcome.NO, 5);

        double qYesBefore = market.getQYes();
        double qNoBefore = market.getQNo();

        // ACT
        market.resolveMarket(Outcome.YES);

        // ASSERT: Shares should be unchanged
        assertEquals(qYesBefore, market.getQYes(), 0.0001,
                "qYes should be preserved after resolution");
        assertEquals(qNoBefore, market.getQNo(), 0.0001,
                "qNo should be preserved after resolution");
    }
}
