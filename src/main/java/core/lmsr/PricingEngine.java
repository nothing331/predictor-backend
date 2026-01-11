package core.lmsr;

import java.math.BigDecimal;

/**
 * LMSR (Logarithmic Market Scoring Rule) Pricing Engine
 * 
 * This class contains ONLY the mathematical formulas for LMSR calculations.
 * It is a pure utility class with no state - all methods are static.
 * 
 * For a stateful market engine that tracks qYes/qNo, see LMSREngine.
 * 
 * Key Formula: C(q) = b × ln(e^(qYes/b) + e^(qNo/b))
 * where:
 * - C(q) = Total cost function (money in the market)
 * - b = Liquidity parameter (higher = more liquidity, less price impact)
 * - qYes = Total YES shares in the market
 * - qNo = Total NO shares in the market
 * 
 * @author Prediction Market Team
 */
public class PricingEngine {

    /** Default liquidity parameter */
    public static final double DEFAULT_LIQUIDITY = 100.0;

    // Private constructor - this is a utility class, not meant to be instantiated
    private PricingEngine() {
    }

    // ======================== CORE COST FUNCTION ========================

    /**
     * LMSR Cost Function: C(q) = b × ln(e^(qYes/b) + e^(qNo/b))
     * 
     * This function represents the total money that has been paid into the market.
     * The cost to buy shares is the difference between cost before and after.
     * 
     * Uses the log-sum-exp trick to prevent numerical overflow for large values.
     * 
     * @param qYes Total YES shares in the market
     * @param qNo  Total NO shares in the market
     * @param b    Liquidity parameter
     * @return The cost function value
     */
    public static BigDecimal costFunction(double qYes, double qNo, double b) {
        // Log-sum-exp trick: log(e^a + e^b) = max(a,b) + log(e^(a-max) + e^(b-max))
        // This prevents overflow when a or b are very large
        double maxQ = Math.max(qYes / b, qNo / b);
        return BigDecimal.valueOf(b * (maxQ + Math.log(Math.exp(qYes / b - maxQ) + Math.exp(qNo / b - maxQ))));
    }

    /**
     * Direct cost function without overflow protection.
     * 
     * Use this for small share values or when debugging.
     * WARNING: Can overflow for large share values (e.g., > 700 with b=100)
     * 
     * @param qYes Total YES shares in the market
     * @param qNo  Total NO shares in the market
     * @param b    Liquidity parameter
     * @return The cost function value
     */
    public static double costFunctionDirect(double qYes, double qNo, double b) {
        return b * Math.log(Math.exp(qYes / b) + Math.exp(qNo / b));
    }

    // ======================== BUYING SHARES ========================

    /**
     * Calculate the cost to buy YES shares.
     * 
     * The cost is: C(qYes + shares, qNo) - C(qYes, qNo)
     * i.e., the difference in the cost function after adding shares.
     * 
     * @param qYes           Current YES shares in market
     * @param qNo            Current NO shares in market
     * @param b              Liquidity parameter
     * @param yesSharesToBuy Number of YES shares to purchase (positive)
     * @return Cost in market currency to buy these shares
     */
    public static BigDecimal calculateYesPrice(double qYes, double qNo, double b, double yesSharesToBuy) {
        return costFunction(qYes + yesSharesToBuy, qNo, b).subtract(costFunction(qYes, qNo, b));
    }

    /**
     * Calculate the cost to buy NO shares.
     * 
     * @param qYes          Current YES shares in market
     * @param qNo           Current NO shares in market
     * @param b             Liquidity parameter
     * @param noSharesToBuy Number of NO shares to purchase (positive)
     * @return Cost in market currency to buy these shares
     */
    public static BigDecimal calculateNoPrice(double qYes, double qNo, double b, double noSharesToBuy) {
        return costFunction(qYes, qNo + noSharesToBuy, b).subtract(costFunction(qYes, qNo, b));
    }

    // ======================== SELLING SHARES ========================

    /**
     * Calculate the payout for selling YES shares.
     * 
     * IMPORTANT: When selling, shares are REMOVED from the pool, which
     * DECREASES the cost function. The payout is the difference.
     * 
     * Formula: Payout = C(qYes, qNo) - C(qYes - sharesSold, qNo)
     * 
     * @param qYes            Current YES shares in market
     * @param qNo             Current NO shares in market
     * @param b               Liquidity parameter
     * @param yesSharesToSell Number of YES shares to sell (positive value)
     * @return Amount the user receives from selling
     */
    public static BigDecimal sellYesPrice(double qYes, double qNo, double b, double yesSharesToSell) {
        return costFunction(qYes, qNo, b).subtract(costFunction(qYes - yesSharesToSell, qNo, b));
    }

    /**
     * Calculate the payout for selling NO shares.
     * 
     * @param qYes           Current YES shares in market
     * @param qNo            Current NO shares in market
     * @param b              Liquidity parameter
     * @param noSharesToSell Number of NO shares to sell (positive value)
     * @return Amount the user receives from selling
     */
    public static BigDecimal sellNoPrice(double qYes, double qNo, double b, double noSharesToSell) {
        return costFunction(qYes, qNo, b).subtract(costFunction(qYes, qNo - noSharesToSell, b));
    }

    // ======================== PRICE / PROBABILITY ========================

    /**
     * Get the current probability/price for YES outcome.
     * 
     * This is derived from the partial derivative of the cost function.
     * Formula: P(YES) = e^(qYes/b) / (e^(qYes/b) + e^(qNo/b))
     * 
     * The result is between 0.0 and 1.0, representing:
     * - The instantaneous price of a YES share
     * - The market's implied probability of YES outcome
     * 
     * @param qYes Current YES shares in market
     * @param qNo  Current NO shares in market
     * @param b    Liquidity parameter
     * @return Probability/price between 0.0 and 1.0
     */
    public static double displayYesPrice(double qYes, double qNo, double b) {
        if (qYes == 0 && qNo == 0) {
            return 0.5; // Initial state: 50/50
        }
        return Math.exp(qYes / b) / (Math.exp(qYes / b) + Math.exp(qNo / b));
    }

    /**
     * Get the current probability/price for NO outcome.
     * 
     * @param qYes Current YES shares in market
     * @param qNo  Current NO shares in market
     * @param b    Liquidity parameter
     * @return Probability/price between 0.0 and 1.0 (always 1 - P(YES))
     */
    public static double displayNoPrice(double qYes, double qNo, double b) {
        return 1 - displayYesPrice(qYes, qNo, b);
    }

    // ! THINGS DO NOT MAKE SENSE HERE
    // ======================== MARKET VALUE ========================

    /**
     * Calculate the current market value of a user's YES shares.
     * 
     * This calculates the liquidation value: how much the user would receive
     * if they sold all their shares immediately.
     * 
     * Formula: Value = C(qYes, qNo) - C(qYes - userShares, qNo)
     * 
     * @param userShares Number of YES shares the user owns
     * @param qYes       Current YES shares in market
     * @param qNo        Current NO shares in market
     * @param b          Liquidity parameter
     * @return Liquidation value
     */
    public static BigDecimal yesShareMarketValue(double userShares, double qYes, double qNo, double b) {
        return sellYesPrice(qYes, qNo, b, userShares);
    }

    /**
     * Calculate the current market value of a user's NO shares.
     * 
     * This calculates the liquidation value: how much the user would receive
     * if they sold all their shares immediately.
     * 
     * @param userShares Number of NO shares the user owns
     * @param qYes       Current YES shares in market
     * @param qNo        Current NO shares in market
     * @param b          Liquidity parameter
     * @return Liquidation value (estimated)
     */
    public static BigDecimal noShareMarketValue(double userShares, double qYes, double qNo, double b) {
        return sellNoPrice(qYes, qNo, b, userShares);
    }

    // ======================== SETTLEMENT ========================

    /**
     * Calculate the final payout at market settlement.
     * 
     * At settlement, winning shares pay out $1 each, losing shares pay $0.
     * 
     * @param yesShares User's YES share holdings
     * @param noShares  User's NO share holdings
     * @param yesWon    True if YES outcome occurred, False if NO
     * @return Final payout amount ($1 per winning share)
     */
    public static double settlementPayout(double yesShares, double noShares, boolean yesWon) {
        return yesWon ? yesShares : noShares;
    }

    // ======================== UTILITY FUNCTIONS ========================

    /**
     * Calculate the maximum subsidy (worst-case loss for market maker).
     * 
     * This is the "bounded loss" property of LMSR - the market maker's
     * maximum possible loss is b × ln(2), regardless of how trading goes.
     * 
     * @param b Liquidity parameter
     * @return Maximum possible loss for the market maker
     */
    public static double getMaxSubsidy(double b) {
        return b * Math.log(2);
    }

    /**
     * Calculate how many shares can be bought for a given amount.
     * 
     * This is the inverse of the cost function, solved via binary search.
     * Useful for "buy max" functionality.
     * 
     * @param amount Money available to spend
     * @param qYes   Current YES shares in market
     * @param qNo    Current NO shares in market
     * @param b      Liquidity parameter
     * @param isYes  True for YES shares, False for NO shares
     * @return Maximum number of shares that can be bought
     */
    public static double sharesForAmount(double amount, double qYes, double qNo, double b, boolean isYes) {
        if (amount <= 0)
            return 0;

        // Binary search for the number of shares
        double low = 0;
        double high = amount * 10; // Upper bound estimation
        double epsilon = 0.0001; // Precision

        while (high - low > epsilon) {
            double mid = (low + high) / 2;
            BigDecimal cost = isYes
                    ? calculateYesPrice(qYes, qNo, b, mid)
                    : calculateNoPrice(qYes, qNo, b, mid);

            if (cost.doubleValue() < amount) {
                low = mid;
            } else {
                high = mid;
            }
        }

        return low;
    }

    /**
     * Validate if a user can afford a trade.
     * 
     * @param sharesToBuy Number of shares to buy
     * @param qYes        Current YES shares in market
     * @param qNo         Current NO shares in market
     * @param b           Liquidity parameter
     * @param isYes       True for YES, False for NO
     * @param userBalance User's available balance
     * @return True if the user can afford the trade
     */
    public static boolean canAffordTrade(double sharesToBuy, double qYes, double qNo, double b,
            boolean isYes, double userBalance) {
        BigDecimal cost = isYes
                ? calculateYesPrice(qYes, qNo, b, sharesToBuy)
                : calculateNoPrice(qYes, qNo, b, sharesToBuy);
        return cost.doubleValue() <= userBalance;
    }
}
