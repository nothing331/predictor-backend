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
 * These tests verify the settlement behavior under the new contract:
 *
 * IMPORTANT: SettlementEngine no longer mutates user.balance.
 * LedgerService.recordSettlementCredit (called by MarketService.resolveMarket)
 * owns all balance writes. The engine's responsibilities are limited to:
 *   - Validating the market is RESOLVED
 *   - Clearing the position (position.clearShares())
 *   - Marking the position settled (position.markAsSettled())
 *   - Throwing on invalid inputs (null/missing/already-settled positions,
 *     unresolved markets, etc.)
 *
 * POSITION TESTS:
 * - Position is cleared (yes/no shares = 0) regardless of outcome
 * - Position is marked settled
 * - Balance is NOT mutated by the engine
 *
 * SAFETY TESTS:
 * - Settling twice throws (idempotency via exception)
 * - settleMarket skips already-settled positions
 * - Cannot settle unresolved market
 * - Null market / missing position throw
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
                @DisplayName("Settling winning YES position does not mutate balance")
                public void testWinningYesShares_BalanceUnchanged() {
                        // ARRANGE
                        double yesShares = 25.0;
                        Position position = user.getOrCreatePosition(market.getMarketId());
                        position.setYesShares(yesShares);
                        position.setNoShares(0.0);

                        BigDecimal initialBalance = user.getBalance();
                        market.resolveMarket(Outcome.YES);

                        // ACT
                        settlementEngine.settleUser(user, market);

                        // ASSERT: balance is NOT mutated by SettlementEngine — LedgerService
                        // owns balance writes via recordSettlementCredit.
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                }

                @Test
                @DisplayName("Settling winning NO position does not mutate balance")
                public void testWinningNoShares_BalanceUnchanged() {
                        // ARRANGE
                        double noShares = 15.5;
                        Position position = user.getOrCreatePosition(market.getMarketId());
                        position.setYesShares(0.0);
                        position.setNoShares(noShares);

                        BigDecimal initialBalance = user.getBalance();
                        market.resolveMarket(Outcome.NO);

                        // ACT
                        settlementEngine.settleUser(user, market);

                        // ASSERT: balance is NOT mutated by SettlementEngine — LedgerService
                        // owns balance writes via recordSettlementCredit.
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                }

                @Test
                @DisplayName("Settling a losing position does not mutate balance")
                public void testLosingShares_BalanceUnchanged() {
                        // ARRANGE: User holds NO shares, but YES wins
                        double noShares = 50.0;
                        Position position = user.getOrCreatePosition(market.getMarketId());
                        position.setYesShares(0.0);
                        position.setNoShares(noShares);

                        BigDecimal initialBalance = user.getBalance();
                        market.resolveMarket(Outcome.YES); // YES wins, NO loses

                        // ACT
                        settlementEngine.settleUser(user, market);

                        // ASSERT: Balance unchanged (engine never mutates balance, and losers earn nothing anyway)
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                }

                @Test
                @DisplayName("Mixed position - SettlementEngine does not mutate balance")
                public void testMixedPosition_BalanceUnchanged() {
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

                        // ASSERT: balance is NOT mutated by SettlementEngine — LedgerService
                        // owns balance writes via recordSettlementCredit.
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance must be unchanged for mixed positions: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                }

                @Test
                @DisplayName("Settlement does not mutate balance")
                public void testSettlementDoesNotMutateBalance() {
                        // ARRANGE
                        BigDecimal initialBalance = new BigDecimal("500.00");
                        user = new User("user-balance", initialBalance);

                        double winningShares = 100.0;
                        Position position = user.getOrCreatePosition(market.getMarketId());
                        position.setYesShares(winningShares);

                        market.resolveMarket(Outcome.YES);

                        // ACT
                        settlementEngine.settleUser(user, market);

                        // ASSERT: balance must remain exactly at the initial value
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance must be unchanged (still 500.00): SettlementEngine does not mutate balance; LedgerService owns balance writes");
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
                @DisplayName("settleMarket does not mutate any user's balance")
                public void testMultipleUsers_BalancesUnchanged() {
                        // ARRANGE
                        User user1 = new User("user-1", new BigDecimal("100.00"));
                        User user2 = new User("user-2", new BigDecimal("200.00"));
                        User user3 = new User("user-3", new BigDecimal("300.00"));

                        user1.getOrCreatePosition(market.getMarketId()).setYesShares(10.0);
                        user2.getOrCreatePosition(market.getMarketId()).setYesShares(20.0);
                        user3.getOrCreatePosition(market.getMarketId()).setYesShares(30.0);

                        market.resolveMarket(Outcome.YES);
                        List<User> users = Arrays.asList(user1, user2, user3);

                        BigDecimal user1Initial = user1.getBalance();
                        BigDecimal user2Initial = user2.getBalance();
                        BigDecimal user3Initial = user3.getBalance();

                        // ACT
                        settlementEngine.settleMarket(market, users);

                        // ASSERT: SettlementEngine does not mutate balance; LedgerService owns balance writes.
                        assertEquals(0, user1Initial.compareTo(user1.getBalance()),
                                        "User1 balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, user2Initial.compareTo(user2.getBalance()),
                                        "User2 balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, user3Initial.compareTo(user3.getBalance()),
                                        "User3 balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                }

                @Test
                @DisplayName("Users with no position have unchanged balance and no error")
                public void testUsersWithNoPosition_BalanceUnchanged() {
                        // ARRANGE
                        User userWithPosition = new User("user-with", new BigDecimal("100.00"));
                        User userWithoutPosition = new User("user-without", new BigDecimal("200.00"));

                        userWithPosition.getOrCreatePosition(market.getMarketId()).setYesShares(50.0);
                        // userWithoutPosition has no position in this market

                        market.resolveMarket(Outcome.YES);
                        List<User> users = Arrays.asList(userWithPosition, userWithoutPosition);

                        BigDecimal balanceWithBefore = userWithPosition.getBalance();
                        BigDecimal balanceWithoutBefore = userWithoutPosition.getBalance();

                        // ACT
                        settlementEngine.settleMarket(market, users);

                        // ASSERT: neither user's balance changes — engine never mutates balance.
                        assertEquals(0, balanceWithBefore.compareTo(userWithPosition.getBalance()),
                                        "User with position must have unchanged balance: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, balanceWithoutBefore.compareTo(userWithoutPosition.getBalance()),
                                        "User without position must have unchanged balance: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                }

                @Test
                @DisplayName("Mixed outcomes - SettlementEngine does not mutate any balance")
                public void testMixedOutcomes_BalancesUnchanged() {
                        // ARRANGE
                        User yesHolder = new User("yes-holder", new BigDecimal("100.00"));
                        User noHolder = new User("no-holder", new BigDecimal("100.00"));

                        yesHolder.getOrCreatePosition(market.getMarketId()).setYesShares(25.0);
                        noHolder.getOrCreatePosition(market.getMarketId()).setNoShares(25.0);

                        market.resolveMarket(Outcome.YES); // YES wins
                        List<User> users = Arrays.asList(yesHolder, noHolder);

                        BigDecimal yesHolderInitial = yesHolder.getBalance();
                        BigDecimal noHolderInitial = noHolder.getBalance();

                        // ACT
                        settlementEngine.settleMarket(market, users);

                        // ASSERT: neither winner nor loser balance changes — engine never mutates balance.
                        assertEquals(0, yesHolderInitial.compareTo(yesHolder.getBalance()),
                                        "YES holder balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, noHolderInitial.compareTo(noHolder.getBalance()),
                                        "NO holder balance must be unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                }
        }

        // ========================================================================
        // SAFETY TESTS
        // ========================================================================

        @Nested
        @DisplayName("Safety Tests")
        class SafetyTests {

                @Test
                @DisplayName("Settling twice throws and balance never changes")
                public void testSettlingTwiceThrows_BalanceNeverChanges() {
                        // ARRANGE
                        Position position = user.getOrCreatePosition(market.getMarketId());
                        position.setYesShares(100.0);

                        BigDecimal initialBalance = user.getBalance();
                        market.resolveMarket(Outcome.YES);

                        // ACT: First settlement
                        settlementEngine.settleUser(user, market);
                        BigDecimal balanceAfterFirstSettlement = user.getBalance();

                        // ASSERT: First settlement clears position and does NOT mutate balance.
                        assertEquals(0, initialBalance.compareTo(balanceAfterFirstSettlement),
                                        "Balance must be unchanged after first settlement: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertTrue(position.isSettled(),
                                        "Position should be marked settled after first settlement");

                        // ACT & ASSERT: Second settlement should throw exception
                        IllegalStateException exception = assertThrows(
                                        IllegalStateException.class,
                                        () -> settlementEngine.settleUser(user, market),
                                        "Second settlement should throw IllegalStateException");

                        assertTrue(exception.getMessage().contains("already settled"),
                                        "Exception message should indicate position is already settled");

                        // Balance never changes — neither the successful first call nor the failed second call mutates it.
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance never changes: SettlementEngine does not mutate balance; LedgerService owns balance writes");
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

                        BigDecimal user1Initial = user1.getBalance();
                        BigDecimal user2Initial = user2.getBalance();

                        // Settle user1 first
                        settlementEngine.settleUser(user1, market);
                        BigDecimal user1BalanceAfterFirst = user1.getBalance();

                        // ACT: Call settleMarket which includes already-settled user1
                        List<User> users = Arrays.asList(user1, user2);
                        settlementEngine.settleMarket(market, users); // Should NOT throw

                        // ASSERT: balance never changes (engine never mutates balance), and
                        // already-settled user1 is silently skipped while user2 is settled.
                        assertEquals(0, user1BalanceAfterFirst.compareTo(user1.getBalance()),
                                        "User1 balance never changes (already settled, skipped): SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, user1Initial.compareTo(user1.getBalance()),
                                        "User1 balance equals initial: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, user2Initial.compareTo(user2.getBalance()),
                                        "User2 balance unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertTrue(user2.getPosition(market.getMarketId()).isSettled(),
                                        "User2 position should be settled normally");
                        assertEquals(0.0, user2.getPosition(market.getMarketId()).getYesShares(), 0.0001,
                                        "User2 position should be cleared");
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

                        // ACT & ASSERT: Try to apply trade (should throw exception)
                        assertThrows(IllegalStateException.class, () -> market.applyTrade(Outcome.YES, 100.0));

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

                        BigDecimal user1Initial = user1.getBalance();
                        BigDecimal user2Initial = user2.getBalance();

                        // ACT
                        settlementEngine.settleUser(user1, market1);
                        settlementEngine.settleUser(user2, market2);

                        // ASSERT: identical setups produce identical (unchanged) balances and identical settled state
                        assertEquals(0, user1.getBalance().compareTo(user2.getBalance()),
                                        "Identical setups should produce identical balances");
                        assertEquals(0, user1Initial.compareTo(user1.getBalance()),
                                        "User1 balance unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, user2Initial.compareTo(user2.getBalance()),
                                        "User2 balance unchanged: SettlementEngine does not mutate balance; LedgerService owns balance writes");
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
                @DisplayName("Very small fractional share positions clear without mutating balance")
                public void testFractionalShares() {
                        // ARRANGE
                        double fractionalShares = 0.001;
                        Position position = user.getOrCreatePosition(market.getMarketId());
                        position.setYesShares(fractionalShares);

                        BigDecimal initialBalance = user.getBalance();
                        market.resolveMarket(Outcome.YES);

                        // ACT
                        settlementEngine.settleUser(user, market);

                        // ASSERT: balance unchanged regardless of share size; position cleared & settled.
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance must be unchanged for fractional shares: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0.0, position.getYesShares(), 0.0001,
                                        "Fractional YES shares should be cleared to 0");
                        assertTrue(position.isSettled(),
                                        "Position should be marked as settled");
                }

                @Test
                @DisplayName("Large positions clear without mutating balance")
                public void testLargeNumberOfShares() {
                        // ARRANGE
                        double largeShares = 1_000_000.0;
                        Position position = user.getOrCreatePosition(market.getMarketId());
                        position.setYesShares(largeShares);

                        BigDecimal initialBalance = user.getBalance();
                        market.resolveMarket(Outcome.YES);

                        // ACT
                        settlementEngine.settleUser(user, market);

                        // ASSERT: balance unchanged regardless of share size; position cleared & settled.
                        assertEquals(0, initialBalance.compareTo(user.getBalance()),
                                        "Balance must be unchanged for large positions: SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0.0, position.getYesShares(), 0.0001,
                                        "Large YES position should be cleared to 0");
                        assertTrue(position.isSettled(),
                                        "Position should be marked as settled");
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
                @DisplayName("Settlement with both outcomes clears position without mutating balance")
                public void testBothOutcomesWork() {
                        // Test YES resolution
                        Market marketYes = new Market("market-yes", "Test YES", "Desc");
                        User userYes = new User("user-yes", new BigDecimal("100.00"));
                        userYes.getOrCreatePosition(marketYes.getMarketId()).setYesShares(50.0);
                        BigDecimal userYesInitial = userYes.getBalance();
                        marketYes.resolveMarket(Outcome.YES);
                        settlementEngine.settleUser(userYes, marketYes);

                        // Test NO resolution
                        Market marketNo = new Market("market-no", "Test NO", "Desc");
                        User userNo = new User("user-no", new BigDecimal("100.00"));
                        userNo.getOrCreatePosition(marketNo.getMarketId()).setNoShares(50.0);
                        BigDecimal userNoInitial = userNo.getBalance();
                        marketNo.resolveMarket(Outcome.NO);
                        settlementEngine.settleUser(userNo, marketNo);

                        // ASSERT: neither balance is mutated; both positions cleared & settled.
                        assertEquals(0, userYesInitial.compareTo(userYes.getBalance()),
                                        "YES winner balance must be unchanged (still 100.00): SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertEquals(0, userNoInitial.compareTo(userNo.getBalance()),
                                        "NO winner balance must be unchanged (still 100.00): SettlementEngine does not mutate balance; LedgerService owns balance writes");
                        assertTrue(userYes.getPosition(marketYes.getMarketId()).isSettled(),
                                        "YES winner position should be settled");
                        assertTrue(userNo.getPosition(marketNo.getMarketId()).isSettled(),
                                        "NO winner position should be settled");
                        assertEquals(0.0, userYes.getPosition(marketYes.getMarketId()).getYesShares(), 0.0001,
                                        "YES winner shares should be cleared");
                        assertEquals(0.0, userNo.getPosition(marketNo.getMarketId()).getNoShares(), 0.0001,
                                        "NO winner shares should be cleared");
                }
        }
}
