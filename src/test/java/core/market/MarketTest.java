package core.market;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for Market class.
 * 
 * Tests cover:
 * 1. Constructor and initialization
 * 2. Price calculations (getYesPrice, getNoPrice)
 * 3. Cost to buy (getCostToBuy)
 * 4. Trade application (applyTrade)
 * 5. Market resolution (resolveMarket)
 * 6. LMSR invariants (prices sum to 1, bounded between 0 and 1)
 * 7. Determinism
 */
public class MarketTest {

    private static final double EPSILON = 1e-9;
    private Market market;

    @BeforeEach
    void setUp() {
        market = new Market("1", "Test Market", "A test prediction market");
    }

    // ======================== CONSTRUCTOR & INITIALIZATION
    // ========================

    @Nested
    @DisplayName("Constructor and Initialization")
    class ConstructorTests {

        @Test
        @DisplayName("New market starts with initial 50/50 prices")
        void newMarketStartsAt50_50() {
            // Act
            double yesPrice = market.getYesPrice();
            double noPrice = market.getNoPrice();

            // Assert
            assertEquals(0.5, yesPrice, EPSILON, "New market YES price should be 0.5");
            assertEquals(0.5, noPrice, EPSILON, "New market NO price should be 0.5");
        }

        @Test
        @DisplayName("New market prices sum to 1")
        void newMarketPricesSumToOne() {
            // Act
            double sum = market.getYesPrice() + market.getNoPrice();

            // Assert
            assertEquals(1.0, sum, EPSILON, "Prices should sum to 1.0");
        }

        @Test
        @DisplayName("Constructor with different parameters creates valid market")
        void constructorWithDifferentParams() {
            // Arrange & Act
            Market customMarket = new Market("42", "Custom Market", "Custom description");

            // Assert
            assertEquals(0.5, customMarket.getYesPrice(), EPSILON);
            assertEquals(0.5, customMarket.getNoPrice(), EPSILON);
        }
    }

    // ======================== PRICE CALCULATIONS ========================

    @Nested
    @DisplayName("Price Calculations")
    class PriceCalculations {

        @Test
        @DisplayName("Prices always sum to 1 after trades")
        void pricesAlwaysSumToOneAfterTrades() {
            // Arrange & Act - apply several trades
            market.applyTrade(Outcome.YES, 50);
            double sum1 = market.getYesPrice() + market.getNoPrice();

            market.applyTrade(Outcome.NO, 75);
            double sum2 = market.getYesPrice() + market.getNoPrice();

            market.applyTrade(Outcome.YES, 100);
            double sum3 = market.getYesPrice() + market.getNoPrice();

            // Assert
            assertAll(
                    () -> assertEquals(1.0, sum1, EPSILON, "Prices should sum to 1 after YES trade"),
                    () -> assertEquals(1.0, sum2, EPSILON, "Prices should sum to 1 after NO trade"),
                    () -> assertEquals(1.0, sum3, EPSILON, "Prices should sum to 1 after multiple trades"));
        }

        @Test
        @DisplayName("Prices are always between 0 and 1")
        void pricesAlwaysBounded() {
            // Arrange & Act - apply various trades
            market.applyTrade(Outcome.YES, 200);
            double yesPrice1 = market.getYesPrice();
            double noPrice1 = market.getNoPrice();

            market.applyTrade(Outcome.NO, 300);
            double yesPrice2 = market.getYesPrice();
            double noPrice2 = market.getNoPrice();

            // Assert
            assertAll(
                    () -> assertTrue(yesPrice1 > 0 && yesPrice1 < 1, "YES price should be in (0,1)"),
                    () -> assertTrue(noPrice1 > 0 && noPrice1 < 1, "NO price should be in (0,1)"),
                    () -> assertTrue(yesPrice2 > 0 && yesPrice2 < 1, "YES price should be in (0,1)"),
                    () -> assertTrue(noPrice2 > 0 && noPrice2 < 1, "NO price should be in (0,1)"));
        }

        @Test
        @DisplayName("Buying YES shares increases YES price")
        void buyingYesIncreasesYesPrice() {
            // Arrange
            double initialYesPrice = market.getYesPrice();

            // Act
            market.applyTrade(Outcome.YES, 50);
            double newYesPrice = market.getYesPrice();

            // Assert
            assertTrue(newYesPrice > initialYesPrice,
                    "Buying YES should increase YES price");
        }

        @Test
        @DisplayName("Buying NO shares increases NO price")
        void buyingNoIncreasesNoPrice() {
            // Arrange
            double initialNoPrice = market.getNoPrice();

            // Act
            market.applyTrade(Outcome.NO, 50);
            double newNoPrice = market.getNoPrice();

            // Assert
            assertTrue(newNoPrice > initialNoPrice,
                    "Buying NO should increase NO price");
        }

        @Test
        @DisplayName("Buying YES shares decreases NO price")
        void buyingYesDecreasesNoPrice() {
            // Arrange
            double initialNoPrice = market.getNoPrice();

            // Act
            market.applyTrade(Outcome.YES, 50);
            double newNoPrice = market.getNoPrice();

            // Assert
            assertTrue(newNoPrice < initialNoPrice,
                    "Buying YES should decrease NO price");
        }

        @Test
        @DisplayName("Buying NO shares decreases YES price")
        void buyingNoDecreasesYesPrice() {
            // Arrange
            double initialYesPrice = market.getYesPrice();

            // Act
            market.applyTrade(Outcome.NO, 50);
            double newYesPrice = market.getYesPrice();

            // Assert
            assertTrue(newYesPrice < initialYesPrice,
                    "Buying NO should decrease YES price");
        }

        @Test
        @DisplayName("getNoPrice is complement of getYesPrice")
        void noPriceIsComplementOfYesPrice() {
            // Arrange
            market.applyTrade(Outcome.YES, 75);
            market.applyTrade(Outcome.NO, 50);

            // Act
            double yesPrice = market.getYesPrice();
            double noPrice = market.getNoPrice();

            // Assert
            assertEquals(1.0 - yesPrice, noPrice, EPSILON,
                    "NO price should equal 1 - YES price");
        }
    }

    // ======================== COST TO BUY ========================

    @Nested
    @DisplayName("Cost To Buy Calculations")
    class CostToBuy {

        @Test
        @DisplayName("getCostToBuy returns positive value for positive shares")
        void costToBuyIsPositive() {
            // Act
            double yesCost = market.getCostToBuy(Outcome.YES, 10).doubleValue();
            double noCost = market.getCostToBuy(Outcome.NO, 10).doubleValue();

            // Assert
            assertTrue(yesCost > 0, "Cost to buy YES shares should be positive");
            assertTrue(noCost > 0, "Cost to buy NO shares should be positive");
        }

        @Test
        @DisplayName("Cost increases with more shares")
        void costIncreasesWithMoreShares() {
            // Act
            double cost10 = market.getCostToBuy(Outcome.YES, 10).doubleValue();
            double cost50 = market.getCostToBuy(Outcome.YES, 50).doubleValue();
            double cost100 = market.getCostToBuy(Outcome.YES, 100).doubleValue();

            // Assert
            assertTrue(cost50 > cost10, "50 shares should cost more than 10");
            assertTrue(cost100 > cost50, "100 shares should cost more than 50");
        }

        @Test
        @DisplayName("Cost is finite for reasonable share amounts")
        void costIsFinite() {
            // Act
            double yesCost = market.getCostToBuy(Outcome.YES, 1000).doubleValue();
            double noCost = market.getCostToBuy(Outcome.NO, 1000).doubleValue();

            // Assert
            assertTrue(Double.isFinite(yesCost), "YES cost should be finite");
            assertTrue(Double.isFinite(noCost), "NO cost should be finite");
        }

        @Test
        @DisplayName("Cost for YES and NO differs based on current prices")
        void costDiffersBasedOnPrices() {
            // Arrange - skew the market
            market.applyTrade(Outcome.YES, 100);

            // Act
            double yesCost = market.getCostToBuy(Outcome.YES, 10).doubleValue();
            double noCost = market.getCostToBuy(Outcome.NO, 10).doubleValue();

            // Assert - YES should cost more now (higher price)
            assertTrue(yesCost > noCost,
                    "With YES favored, buying YES should cost more than NO");
        }

        @ParameterizedTest(name = "Buying {0} shares has positive cost")
        @ValueSource(doubles = { 1, 5, 10, 25, 50, 100, 500 })
        @DisplayName("Various share amounts have positive cost")
        void variousShareAmountsHavePositiveCost(double shares) {
            // Act
            double yesCost = market.getCostToBuy(Outcome.YES, shares).doubleValue();
            double noCost = market.getCostToBuy(Outcome.NO, shares).doubleValue();

            // Assert
            assertAll(
                    () -> assertTrue(yesCost > 0, "YES cost should be positive"),
                    () -> assertTrue(noCost > 0, "NO cost should be positive"));
        }

        @Test
        @DisplayName("Cost reflects market state changes")
        void costReflectsMarketStateChanges() {
            // Arrange
            double initialYesCost = market.getCostToBuy(Outcome.YES, 10).doubleValue();

            // Act - buy YES to increase its price
            market.applyTrade(Outcome.YES, 100);
            double newYesCost = market.getCostToBuy(Outcome.YES, 10).doubleValue();

            // Assert
            assertTrue(newYesCost > initialYesCost,
                    "After buying YES, subsequent YES purchases should cost more");
        }
    }

    // ======================== APPLY TRADE ========================

    @Nested
    @DisplayName("Apply Trade")
    class ApplyTrade {

        @Test
        @DisplayName("Applying YES trade increases YES price")
        void applyYesTradeIncreasesYesPrice() {
            // Arrange
            double beforePrice = market.getYesPrice();

            // Act
            market.applyTrade(Outcome.YES, 25);
            double afterPrice = market.getYesPrice();

            // Assert
            assertTrue(afterPrice > beforePrice,
                    "YES price should increase after buying YES");
        }

        @Test
        @DisplayName("Applying NO trade increases NO price")
        void applyNoTradeIncreasesNoPrice() {
            // Arrange
            double beforePrice = market.getNoPrice();

            // Act
            market.applyTrade(Outcome.NO, 25);
            double afterPrice = market.getNoPrice();

            // Assert
            assertTrue(afterPrice > beforePrice,
                    "NO price should increase after buying NO");
        }

        @Test
        @DisplayName("Applying zero shares throws exception")
        void applyZeroSharesThrowsException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> market.applyTrade(Outcome.YES, 0),
                    "Zero shares should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Applying negative shares throws exception")
        void applyNegativeSharesThrowsException() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> market.applyTrade(Outcome.YES, -10),
                    "Negative shares should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Trade does not change market when resolved")
        void tradeDoesNotChangeWhenResolved() {
            // Arrange
            market.resolveMarket(Outcome.YES);
            double beforeYesPrice = market.getYesPrice();
            double beforeNoPrice = market.getNoPrice();

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> market.applyTrade(Outcome.YES, 100));

            // Assert State Unchanged
            assertEquals(beforeYesPrice, market.getYesPrice(), EPSILON,
                    "YES price should not change after attempted trade on resolved market");
            assertEquals(beforeNoPrice, market.getNoPrice(), EPSILON,
                    "NO price should not change after attempted trade on resolved market");
        }

        @Test
        @DisplayName("Multiple trades accumulate correctly")
        void multipleTradesAccumulate() {
            // Arrange
            double priceAfter0 = market.getYesPrice();

            // Act - apply multiple trades
            market.applyTrade(Outcome.YES, 20);
            double priceAfter1 = market.getYesPrice();

            market.applyTrade(Outcome.YES, 30);
            double priceAfter2 = market.getYesPrice();

            market.applyTrade(Outcome.YES, 50);
            double priceAfter3 = market.getYesPrice();

            // Assert - prices should increase with each trade
            assertTrue(priceAfter1 > priceAfter0, "Price should increase after first trade");
            assertTrue(priceAfter2 > priceAfter1, "Price should increase after second trade");
            assertTrue(priceAfter3 > priceAfter2, "Price should increase after third trade");
        }

        @ParameterizedTest(name = "Trade of {0} shares maintains price invariants")
        @ValueSource(doubles = { 0.1, 1, 10, 100, 1000 })
        @DisplayName("Various trade sizes maintain LMSR invariants")
        void variousTradeSizesMaintainInvariants(double shares) {
            // Act
            market.applyTrade(Outcome.YES, shares);

            // Assert
            double sum = market.getYesPrice() + market.getNoPrice();
            assertEquals(1.0, sum, EPSILON,
                    "Prices should sum to 1 after trade of " + shares + " shares");
        }
    }

    // ======================== MARKET RESOLUTION ========================

    @Nested
    @DisplayName("Market Resolution")
    class MarketResolution {

        @Test
        @DisplayName("Resolving market to YES prevents further trades")
        void resolveToYesPreventsTrades() {
            // Arrange
            market.applyTrade(Outcome.YES, 50);
            double priceBeforeResolve = market.getYesPrice();

            // Act
            market.resolveMarket(Outcome.YES);
            assertThrows(IllegalStateException.class, () -> market.applyTrade(Outcome.NO, 100)); // Should throw

            // Assert
            assertEquals(priceBeforeResolve, market.getYesPrice(), EPSILON,
                    "Price should not change after resolution");
        }

        @Test
        @DisplayName("Resolving market to NO prevents further trades")
        void resolveToNoPreventsTrades() {
            // Arrange
            market.applyTrade(Outcome.NO, 50);
            double priceBeforeResolve = market.getNoPrice();

            // Act
            market.resolveMarket(Outcome.NO);
            assertThrows(IllegalStateException.class, () -> market.applyTrade(Outcome.YES, 100)); // Should throw

            // Assert
            assertEquals(priceBeforeResolve, market.getNoPrice(), EPSILON,
                    "Price should not change after resolution");
        }

        @Test
        @DisplayName("Cannot re-resolve an already resolved market")
        void cannotReResolveMarket() {
            // Arrange
            market.applyTrade(Outcome.YES, 100);
            market.resolveMarket(Outcome.YES);
            double priceAfterFirstResolve = market.getYesPrice();

            // Act & Assert - trying to resolve again should throw
            assertThrows(IllegalStateException.class,
                    () -> market.resolveMarket(Outcome.NO),
                    "Re-resolving should throw IllegalStateException");

            // Trying to apply trade after resolution should throw
            assertThrows(IllegalStateException.class, () -> market.applyTrade(Outcome.NO, 200));

            // Assert - state should not change
            assertEquals(priceAfterFirstResolve, market.getYesPrice(), EPSILON,
                    "Re-resolving attempt should not change market state");
        }

        @Test
        @DisplayName("Resolved market still returns valid prices")
        void resolvedMarketReturnsValidPrices() {
            // Arrange
            market.applyTrade(Outcome.YES, 75);
            market.resolveMarket(Outcome.YES);

            // Act
            double yesPrice = market.getYesPrice();
            double noPrice = market.getNoPrice();

            // Assert
            assertAll(
                    () -> assertTrue(yesPrice > 0 && yesPrice < 1, "YES price should be bounded"),
                    () -> assertTrue(noPrice > 0 && noPrice < 1, "NO price should be bounded"),
                    () -> assertEquals(1.0, yesPrice + noPrice, EPSILON, "Prices should sum to 1"));
        }
    }

    // ======================== DETERMINISM ========================

    @Nested
    @DisplayName("Determinism: Same Operations → Same Results")
    class Determinism {

        @Test
        @DisplayName("Identical markets produce identical prices")
        void identicalMarketsProduceIdenticalPrices() {
            // Arrange
            Market market1 = new Market("1", "Market 1", "Description 1");
            Market market2 = new Market("2", "Market 2", "Description 2");

            // Act - apply same trades to both
            market1.applyTrade(Outcome.YES, 50);
            market2.applyTrade(Outcome.YES, 50);

            market1.applyTrade(Outcome.NO, 30);
            market2.applyTrade(Outcome.NO, 30);

            // Assert
            assertEquals(market1.getYesPrice(), market2.getYesPrice(), 0.0,
                    "Same trades should produce identical YES prices");
            assertEquals(market1.getNoPrice(), market2.getNoPrice(), 0.0,
                    "Same trades should produce identical NO prices");
        }

        @Test
        @DisplayName("Repeated price queries return identical results")
        void repeatedQueriesReturnIdenticalResults() {
            // Arrange
            market.applyTrade(Outcome.YES, 100);
            market.applyTrade(Outcome.NO, 75);

            // Act
            double yesPrice1 = market.getYesPrice();
            double yesPrice2 = market.getYesPrice();
            double yesPrice3 = market.getYesPrice();

            double noPrice1 = market.getNoPrice();
            double noPrice2 = market.getNoPrice();
            double noPrice3 = market.getNoPrice();

            // Assert
            assertAll(
                    () -> assertEquals(yesPrice1, yesPrice2, 0.0, "YES price should be deterministic"),
                    () -> assertEquals(yesPrice2, yesPrice3, 0.0, "YES price should be deterministic"),
                    () -> assertEquals(noPrice1, noPrice2, 0.0, "NO price should be deterministic"),
                    () -> assertEquals(noPrice2, noPrice3, 0.0, "NO price should be deterministic"));
        }

        @Test
        @DisplayName("Repeated cost queries return identical results")
        void repeatedCostQueriesReturnIdenticalResults() {
            // Arrange
            market.applyTrade(Outcome.YES, 50);

            // Act
            double cost1 = market.getCostToBuy(Outcome.YES, 25).doubleValue();
            double cost2 = market.getCostToBuy(Outcome.YES, 25).doubleValue();
            double cost3 = market.getCostToBuy(Outcome.YES, 25).doubleValue();

            // Assert
            assertAll(
                    () -> assertEquals(cost1, cost2, 0.0, "Cost should be deterministic"),
                    () -> assertEquals(cost2, cost3, 0.0, "Cost should be deterministic"));
        }

        @Test
        @DisplayName("Determinism across many iterations")
        void determinismAcrossManyIterations() {
            // Arrange
            market.applyTrade(Outcome.YES, 123.456);
            market.applyTrade(Outcome.NO, 78.9);

            double baselineYes = market.getYesPrice();
            double baselineNo = market.getNoPrice();
            double baselineCost = market.getCostToBuy(Outcome.YES, 50).doubleValue();

            // Assert - 100 iterations should all match baseline
            for (int i = 0; i < 100; i++) {
                assertEquals(baselineYes, market.getYesPrice(), 0.0,
                        "Iteration " + i + ": YES price should match baseline");
                assertEquals(baselineNo, market.getNoPrice(), 0.0,
                        "Iteration " + i + ": NO price should match baseline");
                assertEquals(baselineCost, market.getCostToBuy(Outcome.YES, 50).doubleValue(), 0.0,
                        "Iteration " + i + ": Cost should match baseline");
            }
        }
    }

    // ======================== INTEGRATION / SCENARIOS ========================

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Full trading sequence maintains invariants")
        void fullTradingSequenceMaintainsInvariants() {
            // Act & Assert - run a full sequence
            for (int i = 0; i < 10; i++) {
                if (i % 2 == 0) {
                    market.applyTrade(Outcome.YES, 10 + i * 5);
                } else {
                    market.applyTrade(Outcome.NO, 10 + i * 5);
                }

                // Check invariants after each trade
                double sum = market.getYesPrice() + market.getNoPrice();
                assertEquals(1.0, sum, EPSILON,
                        "Prices should sum to 1 after trade " + i);
                assertTrue(market.getYesPrice() > 0 && market.getYesPrice() < 1,
                        "YES price should be bounded after trade " + i);
                assertTrue(market.getNoPrice() > 0 && market.getNoPrice() < 1,
                        "NO price should be bounded after trade " + i);
            }
        }

        @Test
        @DisplayName("Alternating trades keep prices near 0.5")
        void alternatingTradesKeepPricesNear50() {
            // Act - buy equal amounts of YES and NO alternately
            for (int i = 0; i < 5; i++) {
                market.applyTrade(Outcome.YES, 20);
                market.applyTrade(Outcome.NO, 20);
            }

            // Assert - prices should be near 0.5
            assertEquals(0.5, market.getYesPrice(), 0.01,
                    "Equal trading should keep YES price near 0.5");
            assertEquals(0.5, market.getNoPrice(), 0.01,
                    "Equal trading should keep NO price near 0.5");
        }

        @Test
        @DisplayName("Heavy YES buying pushes price near 1")
        void heavyYesBuyingPushesPriceNear1() {
            // Act
            market.applyTrade(Outcome.YES, 500);

            // Assert
            assertTrue(market.getYesPrice() > 0.99,
                    "Heavy YES buying should push YES price near 1");
            assertTrue(market.getNoPrice() < 0.01,
                    "Heavy YES buying should push NO price near 0");
        }

        @Test
        @DisplayName("Heavy NO buying pushes price near 1")
        void heavyNoBuyingPushesPriceNear1() {
            // Act
            market.applyTrade(Outcome.NO, 500);

            // Assert
            assertTrue(market.getNoPrice() > 0.99,
                    "Heavy NO buying should push NO price near 1");
            assertTrue(market.getYesPrice() < 0.01,
                    "Heavy NO buying should push YES price near 0");
        }
    }
}
