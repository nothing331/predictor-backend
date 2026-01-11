package core.lmsr;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for PricingEngine LMSR calculations.
 * 
 * These tests verify critical invariants of the LMSR pricing model:
 * 1. Prices always sum to 1 (valid probability distribution)
 * 2. Prices are always between 0 and 1 (valid probabilities)
 */
public class PricingEngineTest {

        private static final double EPSILON = 1e-9; // Tolerance for floating-point comparisons

        // ======================== PRICES SUM TO 1 ========================

        @Nested
        @DisplayName("Prices Always Sum to 1")
        class PricesSumToOne {

                @Test
                @DisplayName("Initial state (qYes=0, qNo=0) should sum to 1")
                void initialStateSumsToOne() {
                        // Arrange
                        double qYes = 0;
                        double qNo = 0;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        "YES price + NO price should equal 1.0");
                }

                @Test
                @DisplayName("Equal shares (qYes=qNo) should sum to 1")
                void equalSharesSumToOne() {
                        // Arrange
                        double qYes = 50;
                        double qNo = 50;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        "YES price + NO price should equal 1.0");
                }

                @Test
                @DisplayName("More YES shares than NO should sum to 1")
                void moreYesSharesSumsToOne() {
                        // Arrange
                        double qYes = 150;
                        double qNo = 50;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        "YES price + NO price should equal 1.0");
                }

                @Test
                @DisplayName("More NO shares than YES should sum to 1")
                void moreNoSharesSumsToOne() {
                        // Arrange
                        double qYes = 30;
                        double qNo = 200;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        "YES price + NO price should equal 1.0");
                }

                @ParameterizedTest(name = "qYes={0}, qNo={1}, b={2} should sum to 1")
                @CsvSource({
                                "0, 0, 100",
                                "100, 100, 100",
                                "50, 150, 100",
                                "200, 50, 100",
                                "1, 1000, 100",
                                "1000, 1, 100",
                                "0, 500, 100",
                                "500, 0, 100",
                                "100, 100, 50", // Different liquidity
                                "100, 100, 200", // Different liquidity
                                "100, 100, 10", // Low liquidity
                                "100, 100, 1000", // High liquidity
                                "0.5, 0.5, 1", // Small values
                                "1000, 1000, 500" // Large values
                })
                @DisplayName("Parameterized: Various qYes, qNo, b combinations should sum to 1")
                void variousCombinationsSumToOne(double qYes, double qNo, double b) {
                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        String.format("For qYes=%.2f, qNo=%.2f, b=%.2f: YES(%.6f) + NO(%.6f) should equal 1.0",
                                                        qYes, qNo, b, yesPrice, noPrice));
                }

                @Test
                @DisplayName("Extreme imbalance should still sum to 1")
                void extremeImbalanceSumsToOne() {
                        // Arrange - extreme case where YES is heavily favored
                        double qYes = 500;
                        double qNo = 10;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        "Even with extreme imbalance, prices should sum to 1.0");
                }

                @Test
                @DisplayName("Very small liquidity parameter should sum to 1")
                void smallLiquiditySumsToOne() {
                        // Arrange
                        double qYes = 10;
                        double qNo = 20;
                        double b = 5; // Very small liquidity - prices move quickly

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        "With small liquidity, prices should still sum to 1.0");
                }

                @Test
                @DisplayName("Very large liquidity parameter should sum to 1")
                void largeLiquiditySumsToOne() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 200;
                        double b = 10000; // Very large liquidity - prices move slowly

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertEquals(1.0, yesPrice + noPrice, EPSILON,
                                        "With large liquidity, prices should still sum to 1.0");
                }
        }

        // ======================== PRICES BETWEEN 0 AND 1 ========================

        @Nested
        @DisplayName("Prices Are Always Between 0 and 1")
        class PricesBetweenZeroAndOne {

                @Test
                @DisplayName("Initial state prices should be between 0 and 1")
                void initialStatePricesInRange() {
                        // Arrange
                        double qYes = 0;
                        double qNo = 0;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertAll(
                                        () -> assertTrue(yesPrice > 0, "YES price should be > 0"),
                                        () -> assertTrue(yesPrice < 1, "YES price should be < 1"),
                                        () -> assertTrue(noPrice > 0, "NO price should be > 0"),
                                        () -> assertTrue(noPrice < 1, "NO price should be < 1"));
                }

                @Test
                @DisplayName("Equal shares: prices should be exactly 0.5")
                void equalSharesPricesAtHalf() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 100;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertAll(
                                        () -> assertEquals(0.5, yesPrice, EPSILON, "YES price should be 0.5"),
                                        () -> assertEquals(0.5, noPrice, EPSILON, "NO price should be 0.5"));
                }

                @ParameterizedTest(name = "qYes={0}, qNo={1}, b={2} prices should be in (0, 1)")
                @CsvSource({
                                "0, 0, 100",
                                "100, 100, 100",
                                "50, 150, 100",
                                "200, 50, 100",
                                "1, 1000, 100",
                                "1000, 1, 100",
                                "0, 500, 100",
                                "500, 0, 100",
                                "100, 100, 50",
                                "100, 100, 200",
                                "100, 100, 10",
                                "100, 100, 1000",
                                "0.5, 0.5, 1",
                                "1000, 1000, 500"
                })
                @DisplayName("Parameterized: Various combinations should have prices in (0, 1)")
                void variousCombinationsInRange(double qYes, double qNo, double b) {
                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertAll(
                                        () -> assertTrue(yesPrice > 0,
                                                        String.format("YES price (%.6f) should be > 0 for qYes=%.2f, qNo=%.2f, b=%.2f",
                                                                        yesPrice, qYes, qNo, b)),
                                        () -> assertTrue(yesPrice < 1,
                                                        String.format("YES price (%.6f) should be < 1 for qYes=%.2f, qNo=%.2f, b=%.2f",
                                                                        yesPrice, qYes, qNo, b)),
                                        () -> assertTrue(noPrice > 0,
                                                        String.format("NO price (%.6f) should be > 0 for qYes=%.2f, qNo=%.2f, b=%.2f",
                                                                        noPrice, qYes, qNo, b)),
                                        () -> assertTrue(noPrice < 1,
                                                        String.format("NO price (%.6f) should be < 1 for qYes=%.2f, qNo=%.2f, b=%.2f",
                                                                        noPrice, qYes, qNo, b)));
                }

                @Test
                @DisplayName("Extreme YES dominance: YES price high but still < 1")
                void extremeYesDominance() {
                        // Arrange - YES shares massively outweigh NO
                        double qYes = 500;
                        double qNo = 10;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertAll(
                                        () -> assertTrue(yesPrice > 0.9, "YES price should be very high (>0.9)"),
                                        () -> assertTrue(yesPrice < 1, "YES price should still be < 1"),
                                        () -> assertTrue(noPrice > 0, "NO price should still be > 0"),
                                        () -> assertTrue(noPrice < 0.1, "NO price should be very low (<0.1)"));
                }

                @Test
                @DisplayName("Extreme NO dominance: NO price high but still < 1")
                void extremeNoDominance() {
                        // Arrange - NO shares massively outweigh YES
                        double qYes = 10;
                        double qNo = 500;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertAll(
                                        () -> assertTrue(noPrice > 0.9, "NO price should be very high (>0.9)"),
                                        () -> assertTrue(noPrice < 1, "NO price should still be < 1"),
                                        () -> assertTrue(yesPrice > 0, "YES price should still be > 0"),
                                        () -> assertTrue(yesPrice < 0.1, "YES price should be very low (<0.1)"));
                }

                @Test
                @DisplayName("Very low liquidity with imbalance: prices still bounded")
                void lowLiquidityWithImbalance() {
                        // Arrange - low liquidity means prices move quickly
                        double qYes = 10;
                        double qNo = 50;
                        double b = 5; // Low liquidity

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert - prices should still be strictly between 0 and 1
                        assertAll(
                                        () -> assertTrue(yesPrice > 0, "YES price should be > 0"),
                                        () -> assertTrue(yesPrice < 1, "YES price should be < 1"),
                                        () -> assertTrue(noPrice > 0, "NO price should be > 0"),
                                        () -> assertTrue(noPrice < 1, "NO price should be < 1"));
                }

                @Test
                @DisplayName("Only YES shares (qNo=0): prices still valid")
                void onlyYesShares() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 0;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertAll(
                                        () -> assertTrue(yesPrice > 0, "YES price should be > 0"),
                                        () -> assertTrue(yesPrice < 1, "YES price should be < 1"),
                                        () -> assertTrue(noPrice > 0, "NO price should be > 0"),
                                        () -> assertTrue(noPrice < 1, "NO price should be < 1"));
                }

                @Test
                @DisplayName("Only NO shares (qYes=0): prices still valid")
                void onlyNoShares() {
                        // Arrange
                        double qYes = 0;
                        double qNo = 100;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert
                        assertAll(
                                        () -> assertTrue(yesPrice > 0, "YES price should be > 0"),
                                        () -> assertTrue(yesPrice < 1, "YES price should be < 1"),
                                        () -> assertTrue(noPrice > 0, "NO price should be > 0"),
                                        () -> assertTrue(noPrice < 1, "NO price should be < 1"));
                }

                @Test
                @DisplayName("High liquidity smooths prices towards 0.5")
                void highLiquiditySmoothsPrices() {
                        // Arrange - high liquidity means prices move slowly
                        double qYes = 100;
                        double qNo = 200;
                        double b = 10000; // Very high liquidity

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert - with high liquidity, even 100 share difference barely moves price
                        assertAll(
                                        () -> assertTrue(yesPrice > 0.4,
                                                        "High liquidity should keep YES price close to 0.5"),
                                        () -> assertTrue(yesPrice < 0.6,
                                                        "High liquidity should keep YES price close to 0.5"),
                                        () -> assertTrue(noPrice > 0.4,
                                                        "High liquidity should keep NO price close to 0.5"),
                                        () -> assertTrue(noPrice < 0.6,
                                                        "High liquidity should keep NO price close to 0.5"));
                }
        }

        // ======================== ADDITIONAL EDGE CASES ========================

        @Nested
        @DisplayName("Edge Cases and Boundary Tests")
        class EdgeCases {

                @Test
                @DisplayName("displayNoPrice is always 1 - displayYesPrice")
                void noPriceIsComplement() {
                        // Arrange
                        double qYes = 75;
                        double qNo = 150;
                        double b = 100;

                        // Act
                        double yesPrice = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double noPrice = PricingEngine.displayNoPrice(qYes, qNo, b);
                        double complementPrice = 1 - yesPrice;

                        // Assert
                        assertEquals(complementPrice, noPrice, EPSILON,
                                        "NO price should be exactly 1 - YES price");
                }

                @Test
                @DisplayName("Symmetric prices when shares are swapped")
                void symmetricPricesWhenSwapped() {
                        // Arrange
                        double qYes1 = 100;
                        double qNo1 = 50;
                        double qYes2 = 50; // Swapped
                        double qNo2 = 100; // Swapped
                        double b = 100;

                        // Act
                        double yesPrice1 = PricingEngine.displayYesPrice(qYes1, qNo1, b);
                        double yesPrice2 = PricingEngine.displayYesPrice(qYes2, qNo2, b);

                        // Assert - when we swap qYes and qNo, yes price should become no price
                        assertEquals(yesPrice1, 1 - yesPrice2, EPSILON,
                                        "Swapping qYes/qNo should swap the prices");
                }

                @Test
                @DisplayName("Default liquidity constant is reasonable")
                void defaultLiquidityIsReasonable() {
                        // Assert
                        assertEquals(100.0, PricingEngine.DEFAULT_LIQUIDITY,
                                        "Default liquidity should be 100.0");
                }
        }

        // ======================== COST FUNCTION CORRECTNESS ========================

        @Nested
        @DisplayName("Cost Function Correctness")
        class CostFunctionCorrectness {

                @Test
                @DisplayName("costFunction: Initial state C(0,0,b) = b × ln(2)")
                void costFunctionInitialState() {
                        // Arrange
                        double qYes = 0;
                        double qNo = 0;
                        double b = 100;

                        // Act
                        double cost = PricingEngine.costFunction(qYes, qNo, b).doubleValue();

                        // Assert - C(0,0,b) = b * ln(e^0 + e^0) = b * ln(2)
                        double expectedCost = b * Math.log(2);
                        assertEquals(expectedCost, cost, EPSILON,
                                        "C(0,0,b) should equal b × ln(2)");
                }

                @Test
                @DisplayName("costFunction: Symmetric inputs produce same cost")
                void costFunctionSymmetric() {
                        // Arrange
                        double q1 = 50;
                        double q2 = 100;
                        double b = 100;

                        // Act
                        double cost1 = PricingEngine.costFunction(q1, q2, b).doubleValue();
                        double cost2 = PricingEngine.costFunction(q2, q1, b).doubleValue();

                        // Assert - C(qYes, qNo) should equal C(qNo, qYes)
                        assertEquals(cost1, cost2, EPSILON,
                                        "Cost function should be symmetric: C(qYes,qNo) = C(qNo,qYes)");
                }

                @Test
                @DisplayName("costFunction: Cost increases when adding shares")
                void costFunctionIncreasing() {
                        // Arrange
                        double qYes = 50;
                        double qNo = 50;
                        double b = 100;

                        // Act
                        double costBefore = PricingEngine.costFunction(qYes, qNo, b).doubleValue();
                        double costAfterYes = PricingEngine.costFunction(qYes + 10, qNo, b).doubleValue();
                        double costAfterNo = PricingEngine.costFunction(qYes, qNo + 10, b).doubleValue();

                        // Assert - adding shares always increases cost
                        assertTrue(costAfterYes > costBefore,
                                        "Adding YES shares should increase cost");
                        assertTrue(costAfterNo > costBefore,
                                        "Adding NO shares should increase cost");
                }

                @Test
                @DisplayName("costFunction: Direct and overflow-protected versions match for small values")
                void costFunctionDirectMatchesProtected() {
                        // Arrange - small values that won't overflow
                        double qYes = 50;
                        double qNo = 75;
                        double b = 100;

                        // Act
                        double costProtected = PricingEngine.costFunction(qYes, qNo, b).doubleValue();
                        double costDirect = PricingEngine.costFunctionDirect(qYes, qNo, b);

                        // Assert
                        assertEquals(costDirect, costProtected, EPSILON,
                                        "For small values, protected and direct versions should match");
                }

                @Test
                @DisplayName("costFunction: Cost formula correctness with known values")
                void costFunctionKnownValues() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 100;
                        double b = 100;

                        // Act
                        double cost = PricingEngine.costFunction(qYes, qNo, b).doubleValue();

                        // Assert - C(100,100,100) = 100 * ln(e^1 + e^1) = 100 * ln(2*e) = 100 * (1 +
                        // ln(2))
                        double expected = b * (1 + Math.log(2));
                        assertEquals(expected, cost, EPSILON,
                                        "C(100,100,100) should equal 100 × (1 + ln(2))");
                }

                @Test
                @DisplayName("calculateYesPrice: Cost to buy equals C(new) - C(old)")
                void calculateYesPriceFormula() {
                        // Arrange
                        double qYes = 50;
                        double qNo = 50;
                        double b = 100;
                        double sharesToBuy = 10;

                        // Act
                        double price = PricingEngine.calculateYesPrice(qYes, qNo, b, sharesToBuy).doubleValue();
                        double expectedPrice = PricingEngine.costFunction(qYes + sharesToBuy, qNo, b).doubleValue()
                                        - PricingEngine.costFunction(qYes, qNo, b).doubleValue();

                        // Assert
                        assertEquals(expectedPrice, price, EPSILON,
                                        "Price should equal C(qYes+shares,qNo) - C(qYes,qNo)");
                }

                @Test
                @DisplayName("calculateNoPrice: Cost to buy equals C(new) - C(old)")
                void calculateNoPriceFormula() {
                        // Arrange
                        double qYes = 50;
                        double qNo = 50;
                        double b = 100;
                        double sharesToBuy = 10;

                        // Act
                        double price = PricingEngine.calculateNoPrice(qYes, qNo, b, sharesToBuy).doubleValue();
                        double expectedPrice = PricingEngine.costFunction(qYes, qNo + sharesToBuy, b).doubleValue()
                                        - PricingEngine.costFunction(qYes, qNo, b).doubleValue();

                        // Assert
                        assertEquals(expectedPrice, price, EPSILON,
                                        "Price should equal C(qYes,qNo+shares) - C(qYes,qNo)");
                }

                @Test
                @DisplayName("calculateYesPrice: Price is always positive for positive shares")
                void calculateYesPricePositive() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 100;
                        double b = 100;
                        double sharesToBuy = 25;

                        // Act
                        double price = PricingEngine.calculateYesPrice(qYes, qNo, b, sharesToBuy).doubleValue();

                        // Assert
                        assertTrue(price > 0,
                                        "Cost to buy positive shares should always be positive");
                }

                @Test
                @DisplayName("calculateNoPrice: Price is always positive for positive shares")
                void calculateNoPricePositive() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 100;
                        double b = 100;
                        double sharesToBuy = 25;

                        // Act
                        double price = PricingEngine.calculateNoPrice(qYes, qNo, b, sharesToBuy).doubleValue();

                        // Assert
                        assertTrue(price > 0,
                                        "Cost to buy positive shares should always be positive");
                }

                @Test
                @DisplayName("calculateYesPrice: Buying more shares costs more")
                void calculateYesPriceIncreasingCost() {
                        // Arrange
                        double qYes = 50;
                        double qNo = 50;
                        double b = 100;

                        // Act
                        double price5 = PricingEngine.calculateYesPrice(qYes, qNo, b, 5).doubleValue();
                        double price10 = PricingEngine.calculateYesPrice(qYes, qNo, b, 10).doubleValue();
                        double price20 = PricingEngine.calculateYesPrice(qYes, qNo, b, 20).doubleValue();

                        // Assert
                        assertTrue(price10 > price5, "Buying 10 shares should cost more than 5");
                        assertTrue(price20 > price10, "Buying 20 shares should cost more than 10");
                }

                @Test
                @DisplayName("calculateNoPrice: Buying more shares costs more")
                void calculateNoPriceIncreasingCost() {
                        // Arrange
                        double qYes = 50;
                        double qNo = 50;
                        double b = 100;

                        // Act
                        double price5 = PricingEngine.calculateNoPrice(qYes, qNo, b, 5).doubleValue();
                        double price10 = PricingEngine.calculateNoPrice(qYes, qNo, b, 10).doubleValue();
                        double price20 = PricingEngine.calculateNoPrice(qYes, qNo, b, 20).doubleValue();

                        // Assert
                        assertTrue(price10 > price5, "Buying 10 shares should cost more than 5");
                        assertTrue(price20 > price10, "Buying 20 shares should cost more than 10");
                }

                @ParameterizedTest(name = "qYes={0}, qNo={1}, b={2}, shares={3}")
                @CsvSource({
                                "0, 0, 100, 10",
                                "50, 50, 100, 25",
                                "100, 200, 100, 50",
                                "0, 500, 100, 100",
                                "500, 0, 100, 100",
                                "100, 100, 50, 30",
                                "100, 100, 200, 30"
                })
                @DisplayName("Parameterized: Cost functions work across various scenarios")
                void costFunctionsVariousScenarios(double qYes, double qNo, double b, double shares) {
                        // Act
                        double yesPrice = PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue();
                        double noPrice = PricingEngine.calculateNoPrice(qYes, qNo, b, shares).doubleValue();

                        // Assert - both prices should be positive and finite
                        assertAll(
                                        () -> assertTrue(yesPrice > 0, "YES price should be positive"),
                                        () -> assertTrue(Double.isFinite(yesPrice), "YES price should be finite"),
                                        () -> assertTrue(noPrice > 0, "NO price should be positive"),
                                        () -> assertTrue(Double.isFinite(noPrice), "NO price should be finite"));
                }

                @Test
                @DisplayName("costFunction: Handles large share values (overflow protection)")
                void costFunctionHandlesLargeValues() {
                        // Arrange - values that would overflow without protection
                        double qYes = 50000;
                        double qNo = 50000;
                        double b = 100;

                        // Act
                        double cost = PricingEngine.costFunction(qYes, qNo, b).doubleValue();

                        // Assert - should not overflow to Infinity
                        assertTrue(Double.isFinite(cost),
                                        "Cost function should handle large values without overflow");
                        assertTrue(cost > 0,
                                        "Cost should still be positive for large values");
                }
        }

        // ======================== DETERMINISM ========================

        @Nested
        @DisplayName("Determinism: Same Input → Same Output")
        class Determinism {

                @Test
                @DisplayName("costFunction: Repeated calls return identical results")
                void costFunctionDeterministic() {
                        // Arrange
                        double qYes = 75;
                        double qNo = 125;
                        double b = 100;

                        // Act - call multiple times
                        double cost1 = PricingEngine.costFunction(qYes, qNo, b).doubleValue();
                        double cost2 = PricingEngine.costFunction(qYes, qNo, b).doubleValue();
                        double cost3 = PricingEngine.costFunction(qYes, qNo, b).doubleValue();

                        // Assert - all calls should return exactly the same value
                        assertAll(
                                        () -> assertEquals(cost1, cost2, 0.0,
                                                        "First and second calls should be identical"),
                                        () -> assertEquals(cost2, cost3, 0.0,
                                                        "Second and third calls should be identical"),
                                        () -> assertEquals(cost1, cost3, 0.0,
                                                        "First and third calls should be identical"));
                }

                @Test
                @DisplayName("calculateYesPrice: Repeated calls return identical results")
                void calculateYesPriceDeterministic() {
                        // Arrange
                        double qYes = 50;
                        double qNo = 100;
                        double b = 100;
                        double shares = 15;

                        // Act - call multiple times
                        double price1 = PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue();
                        double price2 = PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue();
                        double price3 = PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue();

                        // Assert - all calls should return exactly the same value
                        assertAll(
                                        () -> assertEquals(price1, price2, 0.0,
                                                        "First and second calls should be identical"),
                                        () -> assertEquals(price2, price3, 0.0,
                                                        "Second and third calls should be identical"));
                }

                @Test
                @DisplayName("calculateNoPrice: Repeated calls return identical results")
                void calculateNoPriceDeterministic() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 50;
                        double b = 100;
                        double shares = 20;

                        // Act - call multiple times
                        double price1 = PricingEngine.calculateNoPrice(qYes, qNo, b, shares).doubleValue();
                        double price2 = PricingEngine.calculateNoPrice(qYes, qNo, b, shares).doubleValue();
                        double price3 = PricingEngine.calculateNoPrice(qYes, qNo, b, shares).doubleValue();

                        // Assert - all calls should return exactly the same value
                        assertAll(
                                        () -> assertEquals(price1, price2, 0.0,
                                                        "First and second calls should be identical"),
                                        () -> assertEquals(price2, price3, 0.0,
                                                        "Second and third calls should be identical"));
                }

                @Test
                @DisplayName("displayYesPrice: Repeated calls return identical results")
                void displayYesPriceDeterministic() {
                        // Arrange
                        double qYes = 200;
                        double qNo = 100;
                        double b = 100;

                        // Act - call multiple times
                        double price1 = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double price2 = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double price3 = PricingEngine.displayYesPrice(qYes, qNo, b);

                        // Assert - all calls should return exactly the same value
                        assertAll(
                                        () -> assertEquals(price1, price2, 0.0,
                                                        "First and second calls should be identical"),
                                        () -> assertEquals(price2, price3, 0.0,
                                                        "Second and third calls should be identical"));
                }

                @Test
                @DisplayName("displayNoPrice: Repeated calls return identical results")
                void displayNoPriceDeterministic() {
                        // Arrange
                        double qYes = 100;
                        double qNo = 200;
                        double b = 100;

                        // Act - call multiple times
                        double price1 = PricingEngine.displayNoPrice(qYes, qNo, b);
                        double price2 = PricingEngine.displayNoPrice(qYes, qNo, b);
                        double price3 = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Assert - all calls should return exactly the same value
                        assertAll(
                                        () -> assertEquals(price1, price2, 0.0,
                                                        "First and second calls should be identical"),
                                        () -> assertEquals(price2, price3, 0.0,
                                                        "Second and third calls should be identical"));
                }

                @Test
                @DisplayName("All pricing methods: Consistent results in sequence")
                void allMethodsDeterministicInSequence() {
                        // Arrange
                        double qYes = 150;
                        double qNo = 75;
                        double b = 100;
                        double shares = 10;

                        // Act - first round
                        double costA = PricingEngine.costFunction(qYes, qNo, b).doubleValue();
                        double yesPriceA = PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue();
                        double noPriceA = PricingEngine.calculateNoPrice(qYes, qNo, b, shares).doubleValue();
                        double displayYesA = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double displayNoA = PricingEngine.displayNoPrice(qYes, qNo, b);

                        // Act - second round (interleaved order)
                        double displayNoB = PricingEngine.displayNoPrice(qYes, qNo, b);
                        double noPriceB = PricingEngine.calculateNoPrice(qYes, qNo, b, shares).doubleValue();
                        double displayYesB = PricingEngine.displayYesPrice(qYes, qNo, b);
                        double yesPriceB = PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue();
                        double costB = PricingEngine.costFunction(qYes, qNo, b).doubleValue();

                        // Assert - results should match regardless of call order
                        assertAll(
                                        () -> assertEquals(costA, costB, 0.0, "costFunction should be deterministic"),
                                        () -> assertEquals(yesPriceA, yesPriceB, 0.0,
                                                        "calculateYesPrice should be deterministic"),
                                        () -> assertEquals(noPriceA, noPriceB, 0.0,
                                                        "calculateNoPrice should be deterministic"),
                                        () -> assertEquals(displayYesA, displayYesB, 0.0,
                                                        "displayYesPrice should be deterministic"),
                                        () -> assertEquals(displayNoA, displayNoB, 0.0,
                                                        "displayNoPrice should be deterministic"));
                }

                @Test
                @DisplayName("Determinism across many iterations")
                void determinismManyIterations() {
                        // Arrange
                        double qYes = 123.456;
                        double qNo = 789.012;
                        double b = 100;
                        double shares = 50;

                        // Act - get baseline
                        double baselineCost = PricingEngine.costFunction(qYes, qNo, b).doubleValue();
                        double baselineYesPrice = PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue();

                        // Assert - 100 iterations should all match baseline
                        for (int i = 0; i < 100; i++) {
                                assertEquals(baselineCost, PricingEngine.costFunction(qYes, qNo, b).doubleValue(), 0.0,
                                                "Iteration " + i + ": costFunction should match baseline");
                                assertEquals(baselineYesPrice,
                                                PricingEngine.calculateYesPrice(qYes, qNo, b, shares).doubleValue(),
                                                0.0,
                                                "Iteration " + i + ": calculateYesPrice should match baseline");
                        }
                }
        }
}
