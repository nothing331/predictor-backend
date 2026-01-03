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
                    () -> assertTrue(yesPrice > 0.4, "High liquidity should keep YES price close to 0.5"),
                    () -> assertTrue(yesPrice < 0.6, "High liquidity should keep YES price close to 0.5"),
                    () -> assertTrue(noPrice > 0.4, "High liquidity should keep NO price close to 0.5"),
                    () -> assertTrue(noPrice < 0.6, "High liquidity should keep NO price close to 0.5"));
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
}
