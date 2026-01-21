package core.settlement;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.user.Position;
import core.user.User;

/**
 * Settlement Engine Tests - Week 3 Mandatory Tests
 * 
 * These tests verify the settlement behavior:
 * 
 * PAYOUT TESTS:
 * - Winning YES shares pay exactly shares × 1
 * - Losing shares pay 0
 * - User balance increases correctly
 * - Users with no position receive 0
 * - Settlement is deterministic
 * 
 * SAFETY TESTS:
 * - Settling twice does not pay twice
 * - Trades after resolution fail
 */
public class SettlementEngineTest {

    private SettlementEngine settlementEngine;
    private Market market;
    private User user;

    @BeforeEach
    public void setUp() {
        settlementEngine = new SettlementEngine();
        market = new Market("market-1", "Will it rain tomorrow?", "Weather prediction market");
        user = new User("user-1", new BigDecimal("1000.00"));
    }

    // ========================================================================
    // PAYOUT TESTS - SINGLE USER
    // ========================================================================

    @Nested
    @DisplayName("Single User Payout Tests")
    class SingleUserPayoutTests {

        @Test
        @DisplayName("Winning YES shares pay exactly shares × 1")
        public void testWinningYesShares_PayExactly() {
            // ARRANGE
            double yesShares = 25.0;
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(yesShares);
            position.setNoShares(0.0);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.YES);

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT
            BigDecimal expectedPayout = BigDecimal.valueOf(yesShares);
            BigDecimal expectedFinalBalance = initialBalance.add(expectedPayout);

            assertEquals(0, expectedFinalBalance.compareTo(user.getBalance()),
                    "Balance should increase by exactly number of winning shares (25 × 1 = 25)");
        }

        @Test
        @DisplayName("Winning NO shares pay exactly shares × 1")
        public void testWinningNoShares_PayExactly() {
            // ARRANGE
            double noShares = 15.5;
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(0.0);
            position.setNoShares(noShares);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.NO);

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT
            BigDecimal expectedPayout = BigDecimal.valueOf(noShares);
            BigDecimal expectedFinalBalance = initialBalance.add(expectedPayout);

            assertEquals(0, expectedFinalBalance.compareTo(user.getBalance()),
                    "Balance should increase by exactly number of winning shares (15.5 × 1 = 15.5)");
        }

        @Test
        @DisplayName("Losing shares pay 0")
        public void testLosingShares_PayZero() {
            // ARRANGE: User holds NO shares, but YES wins
            double noShares = 50.0;
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(0.0);
            position.setNoShares(noShares);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.YES); // YES wins, NO loses

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT: Balance unchanged (losing shares pay 0)
            assertEquals(0, initialBalance.compareTo(user.getBalance()),
                    "Balance should be unchanged - losing shares pay 0");
        }

        @Test
        @DisplayName("Mixed position - only winning shares pay out")
        public void testMixedPosition_OnlyWinningSharesPay() {
            // ARRANGE: User holds both YES and NO shares
            double yesShares = 30.0;
            double noShares = 20.0;
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(yesShares);
            position.setNoShares(noShares);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.YES); // YES wins

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT: Only YES shares pay out
            BigDecimal expectedPayout = BigDecimal.valueOf(yesShares); // 30 × 1 = 30
            BigDecimal expectedFinalBalance = initialBalance.add(expectedPayout);

            assertEquals(0, expectedFinalBalance.compareTo(user.getBalance()),
                    "Only winning YES shares should pay out (30), losing NO shares pay 0");
        }

        @Test
        @DisplayName("User balance increases correctly after settlement")
        public void testUserBalanceIncreasesCorrectly() {
            // ARRANGE
            BigDecimal initialBalance = new BigDecimal("500.00");
            user = new User("user-balance", initialBalance);

            double winningShares = 100.0;
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(winningShares);

            market.resolveMarket(Outcome.YES);

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT
            BigDecimal expectedBalance = new BigDecimal("600.00"); // 500 + 100
            assertEquals(0, expectedBalance.compareTo(user.getBalance()),
                    "Balance should be initial (500) + payout (100) = 600");
        }

        @Test
        @DisplayName("Position is cleared after settlement")
        public void testPositionClearedAfterSettlement() {
            // ARRANGE
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(50.0);
            position.setNoShares(30.0);

            market.resolveMarket(Outcome.YES);

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT
            assertEquals(0.0, position.getYesShares(), 0.0001,
                    "YES shares should be cleared to 0");
            assertEquals(0.0, position.getNoShares(), 0.0001,
                    "NO shares should be cleared to 0");
            assertTrue(position.isSettled(),
                    "Position should be marked as settled");
        }
    }

    // ========================================================================
    // PAYOUT TESTS - MULTIPLE USERS
    // ========================================================================

    @Nested
    @DisplayName("Multiple Users Payout Tests")
    class MultipleUsersPayoutTests {

        @Test
        @DisplayName("Multiple users with winning positions all receive payouts")
        public void testMultipleUsersWithWinningPositions() {
            // ARRANGE
            User user1 = new User("user-1", new BigDecimal("100.00"));
            User user2 = new User("user-2", new BigDecimal("200.00"));
            User user3 = new User("user-3", new BigDecimal("300.00"));

            user1.getOrCreatePosition(market.getMarketId()).setYesShares(10.0);
            user2.getOrCreatePosition(market.getMarketId()).setYesShares(20.0);
            user3.getOrCreatePosition(market.getMarketId()).setYesShares(30.0);

            market.resolveMarket(Outcome.YES);
            List<User> users = Arrays.asList(user1, user2, user3);

            // ACT
            settlementEngine.settleMarket(market, users);

            // ASSERT
            assertEquals(0, new BigDecimal("110.00").compareTo(user1.getBalance()),
                    "User1: 100 + 10 = 110");
            assertEquals(0, new BigDecimal("220.00").compareTo(user2.getBalance()),
                    "User2: 200 + 20 = 220");
            assertEquals(0, new BigDecimal("330.00").compareTo(user3.getBalance()),
                    "User3: 300 + 30 = 330");
        }

        @Test
        @DisplayName("Users with no position receive 0 payout")
        public void testUsersWithNoPosition_ReceiveZero() {
            // ARRANGE
            User userWithPosition = new User("user-with", new BigDecimal("100.00"));
            User userWithoutPosition = new User("user-without", new BigDecimal("200.00"));

            userWithPosition.getOrCreatePosition(market.getMarketId()).setYesShares(50.0);
            // userWithoutPosition has no position in this market

            market.resolveMarket(Outcome.YES);
            List<User> users = Arrays.asList(userWithPosition, userWithoutPosition);

            BigDecimal balanceWithoutBefore = userWithoutPosition.getBalance();

            // ACT
            settlementEngine.settleMarket(market, users);

            // ASSERT
            assertEquals(0, new BigDecimal("150.00").compareTo(userWithPosition.getBalance()),
                    "User with position should receive payout");
            assertEquals(0, balanceWithoutBefore.compareTo(userWithoutPosition.getBalance()),
                    "User without position should have unchanged balance");
        }

        @Test
        @DisplayName("Mixed outcomes - winners get paid, losers get nothing")
        public void testMixedOutcomes_WinnersVsLosers() {
            // ARRANGE
            User yesHolder = new User("yes-holder", new BigDecimal("100.00"));
            User noHolder = new User("no-holder", new BigDecimal("100.00"));

            yesHolder.getOrCreatePosition(market.getMarketId()).setYesShares(25.0);
            noHolder.getOrCreatePosition(market.getMarketId()).setNoShares(25.0);

            market.resolveMarket(Outcome.YES); // YES wins
            List<User> users = Arrays.asList(yesHolder, noHolder);

            // ACT
            settlementEngine.settleMarket(market, users);

            // ASSERT
            assertEquals(0, new BigDecimal("125.00").compareTo(yesHolder.getBalance()),
                    "YES holder should receive payout (100 + 25)");
            assertEquals(0, new BigDecimal("100.00").compareTo(noHolder.getBalance()),
                    "NO holder should receive nothing (balance unchanged)");
        }
    }

    // ========================================================================
    // SAFETY TESTS
    // ========================================================================

    @Nested
    @DisplayName("Safety Tests")
    class SafetyTests {

        @Test
        @DisplayName("Settling twice does not pay twice - throws exception")
        public void testSettlingTwiceDoesNotPayTwice() {
            // ARRANGE
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(100.0);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.YES);

            // ACT: First settlement
            settlementEngine.settleUser(user, market);
            BigDecimal balanceAfterFirstSettlement = user.getBalance();

            // ASSERT: First settlement worked
            assertEquals(0, initialBalance.add(new BigDecimal("100")).compareTo(balanceAfterFirstSettlement),
                    "First settlement should add 100 to balance");

            // ACT & ASSERT: Second settlement should throw exception
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> settlementEngine.settleUser(user, market),
                    "Second settlement should throw IllegalStateException");

            assertTrue(exception.getMessage().contains("already settled"),
                    "Exception message should indicate position is already settled");

            // Balance should be unchanged after failed second settlement
            assertEquals(0, balanceAfterFirstSettlement.compareTo(user.getBalance()),
                    "Balance should be unchanged after failed second settlement");
        }

        @Test
        @DisplayName("settleMarket skips already settled positions safely")
        public void testSettleMarket_SkipsAlreadySettledPositions() {
            // ARRANGE
            User user1 = new User("user-1", new BigDecimal("100.00"));
            User user2 = new User("user-2", new BigDecimal("100.00"));

            user1.getOrCreatePosition(market.getMarketId()).setYesShares(50.0);
            user2.getOrCreatePosition(market.getMarketId()).setYesShares(50.0);

            market.resolveMarket(Outcome.YES);

            // Settle user1 first
            settlementEngine.settleUser(user1, market);
            BigDecimal user1BalanceAfterFirst = user1.getBalance();

            // ACT: Call settleMarket which includes already-settled user1
            List<User> users = Arrays.asList(user1, user2);
            settlementEngine.settleMarket(market, users); // Should NOT throw

            // ASSERT
            assertEquals(0, user1BalanceAfterFirst.compareTo(user1.getBalance()),
                    "User1 balance should be unchanged (already settled, skipped)");
            assertEquals(0, new BigDecimal("150.00").compareTo(user2.getBalance()),
                    "User2 should be settled normally");
        }

        @Test
        @DisplayName("Trades after resolution fail - market is not OPEN")
        public void testTradesAfterResolutionFail() {
            // ARRANGE
            market.resolveMarket(Outcome.YES);

            // ACT & ASSERT: Attempting to modify shares should fail
            assertThrows(
                    IllegalStateException.class,
                    () -> market.setQYes(100.0),
                    "Should not be able to set qYes on resolved market");

            assertThrows(
                    IllegalStateException.class,
                    () -> market.setQNo(100.0),
                    "Should not be able to set qNo on resolved market");
        }

        @Test
        @DisplayName("applyTrade has no effect after resolution")
        public void testApplyTradeNoEffectAfterResolution() {
            // ARRANGE
            double qYesBefore = market.getQYes();
            double qNoBefore = market.getQNo();

            market.resolveMarket(Outcome.YES);

            // ACT: Try to apply trade (this should have no effect due to guard)
            market.applyTrade(Outcome.YES, 100.0); // Should be silently ignored

            // ASSERT
            assertEquals(qYesBefore, market.getQYes(), 0.0001,
                    "qYes should be unchanged after applyTrade on resolved market");
            assertEquals(qNoBefore, market.getQNo(), 0.0001,
                    "qNo should be unchanged after applyTrade on resolved market");
        }

        @Test
        @DisplayName("Cannot settle unresolved market")
        public void testCannotSettleUnresolvedMarket() {
            // ARRANGE: Market is still OPEN
            assertEquals(MarketStatus.OPEN, market.getStatus());

            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(50.0);

            // ACT & ASSERT
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> settlementEngine.settleUser(user, market),
                    "Should not be able to settle user on unresolved market");

            assertTrue(exception.getMessage().contains("must be resolved"),
                    "Exception should indicate market must be resolved");
        }

        @Test
        @DisplayName("Settlement is deterministic - same inputs produce same outputs")
        public void testSettlementIsDeterministic() {
            // ARRANGE: Create two identical setups
            Market market1 = new Market("market-det-1", "Test", "Desc");
            Market market2 = new Market("market-det-2", "Test", "Desc");

            User user1 = new User("user-det-1", new BigDecimal("500.00"));
            User user2 = new User("user-det-2", new BigDecimal("500.00"));

            user1.getOrCreatePosition(market1.getMarketId()).setYesShares(75.0);
            user2.getOrCreatePosition(market2.getMarketId()).setYesShares(75.0);

            market1.resolveMarket(Outcome.YES);
            market2.resolveMarket(Outcome.YES);

            // ACT
            settlementEngine.settleUser(user1, market1);
            settlementEngine.settleUser(user2, market2);

            // ASSERT
            assertEquals(0, user1.getBalance().compareTo(user2.getBalance()),
                    "Identical setups should produce identical balances");
            assertEquals(user1.getPosition(market1.getMarketId()).isSettled(),
                    user2.getPosition(market2.getMarketId()).isSettled(),
                    "Identical setups should produce identical settled states");
        }
    }

    // ========================================================================
    // EDGE CASES
    // ========================================================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("User with exactly 0 shares receives 0 payout")
        public void testZeroShares_ZeroPayout() {
            // ARRANGE
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(0.0);
            position.setNoShares(0.0);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.YES);

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT
            assertEquals(0, initialBalance.compareTo(user.getBalance()),
                    "User with 0 shares should receive 0 payout");
        }

        @Test
        @DisplayName("Very small fractional shares are handled correctly")
        public void testFractionalShares() {
            // ARRANGE
            double fractionalShares = 0.001;
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(fractionalShares);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.YES);

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT
            BigDecimal expectedPayout = BigDecimal.valueOf(fractionalShares);
            BigDecimal expectedBalance = initialBalance.add(expectedPayout);

            assertEquals(0, expectedBalance.compareTo(user.getBalance()),
                    "Fractional shares should be paid correctly");
        }

        @Test
        @DisplayName("Large number of shares are handled correctly")
        public void testLargeNumberOfShares() {
            // ARRANGE
            double largeShares = 1_000_000.0;
            Position position = user.getOrCreatePosition(market.getMarketId());
            position.setYesShares(largeShares);

            BigDecimal initialBalance = user.getBalance();
            market.resolveMarket(Outcome.YES);

            // ACT
            settlementEngine.settleUser(user, market);

            // ASSERT
            BigDecimal expectedPayout = BigDecimal.valueOf(largeShares);
            BigDecimal expectedBalance = initialBalance.add(expectedPayout);

            assertEquals(0, expectedBalance.compareTo(user.getBalance()),
                    "Large number of shares should be paid correctly");
        }

        @Test
        @DisplayName("Empty user collection is handled gracefully")
        public void testEmptyUserCollection() {
            // ARRANGE
            market.resolveMarket(Outcome.YES);
            List<User> emptyUsers = Collections.emptyList();

            // ACT & ASSERT: Should not throw
            assertDoesNotThrow(
                    () -> settlementEngine.settleMarket(market, emptyUsers),
                    "Empty user collection should be handled gracefully");
        }

        @Test
        @DisplayName("Null market throws IllegalArgumentException")
        public void testNullMarket_ThrowsException() {
            // ACT & ASSERT
            assertThrows(
                    IllegalArgumentException.class,
                    () -> settlementEngine.settleMarket(null, Arrays.asList(user)),
                    "Null market should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("User without position in market throws exception on direct settle")
        public void testUserWithoutPosition_ThrowsException() {
            // ARRANGE: User has no position
            User userNoPosition = new User("no-position", new BigDecimal("100.00"));
            market.resolveMarket(Outcome.YES);

            // ACT & ASSERT
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> settlementEngine.settleUser(userNoPosition, market),
                    "User without position should throw exception on direct settle");

            assertTrue(exception.getMessage().contains("no position"),
                    "Exception should indicate user has no position");
        }

        @Test
        @DisplayName("Settlement with both outcomes works correctly")
        public void testBothOutcomesWork() {
            // Test YES resolution
            Market marketYes = new Market("market-yes", "Test YES", "Desc");
            User userYes = new User("user-yes", new BigDecimal("100.00"));
            userYes.getOrCreatePosition(marketYes.getMarketId()).setYesShares(50.0);
            marketYes.resolveMarket(Outcome.YES);
            settlementEngine.settleUser(userYes, marketYes);

            // Test NO resolution
            Market marketNo = new Market("market-no", "Test NO", "Desc");
            User userNo = new User("user-no", new BigDecimal("100.00"));
            userNo.getOrCreatePosition(marketNo.getMarketId()).setNoShares(50.0);
            marketNo.resolveMarket(Outcome.NO);
            settlementEngine.settleUser(userNo, marketNo);

            // ASSERT: Both should have same final balance
            assertEquals(0, new BigDecimal("150.00").compareTo(userYes.getBalance()),
                    "YES winner should have 100 + 50 = 150");
            assertEquals(0, new BigDecimal("150.00").compareTo(userNo.getBalance()),
                    "NO winner should have 100 + 50 = 150");
        }
    }
}
