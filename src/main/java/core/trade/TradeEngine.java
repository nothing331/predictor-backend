package core.trade;

import java.math.BigDecimal;

import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.user.Position;
import core.user.User;

/**
 * TradeEngine - The Arbiter of Fairness
 * 
 * This is the ONLY place where:
 * - Money moves
 * - Shares are minted
 * - Positions change
 * 
 * All trades follow the exact 5-step atomic process:
 * 1. Calculate cost (pure read, no mutation)
 * 2. Validate balance (guard rail, fail before any mutation)
 * 3. Apply market update (increase qYes or qNo)
 * 4. Deduct user balance (exact cost, no rounding)
 * 5. Update user position (add shares to user's position)
 * 
 * If ANY step fails, NOTHING changes. This is atomicity.
 */
public class TradeEngine {

    public TradeEngine() {
    }

    public Trade executeTrade(User user, Market market, Outcome outcome, int sharesToBuy) {
        // Input validation
        if (sharesToBuy <= 0) {
            throw new IllegalArgumentException("Shares to buy must be positive, got: " + sharesToBuy);
        }

        // ========== STEP 1: CALCULATE COST (Pure Read) ==========
        BigDecimal tradeCost = getTradeCost(market, outcome, sharesToBuy);

        // ========== STEP 2: VALIDATE BALANCE (Guard Rail) ==========
        validateTrade(user, market, tradeCost);

        // ========== STEP 3: APPLY MARKET SHARE UPDATE ==========
        marketShareUpdate(market, outcome, sharesToBuy);

        // ========== STEP 4: DEDUCT USER BALANCE ==========
        userBalanceUpdate(user, tradeCost);

        // ========== STEP 5: UPDATE USER POSITION ==========
        updateUserPosition(user, market, outcome, sharesToBuy);

        // ========== CREATE TRADE RECORD ==========
        return new Trade(
                user.getUserId(),
                market.getMarketId(),
                outcome,
                sharesToBuy,
                tradeCost,
                java.time.Instant.now());
    }

    public BigDecimal getTradeCost(Market market, Outcome outcome, int sharesToBuy) {
        return market.getCostToBuy(outcome, sharesToBuy);
    }

    public void validateTrade(User user, Market market, BigDecimal tradeCost) {
        // Market must be open
        if (market.getStatus() != MarketStatus.OPEN) {
            throw new IllegalStateException("Market is not open for trading: " + market.getMarketId());
        }

        // User must have sufficient balance (using BigDecimal.compareTo)
        if (user.getBalance().compareTo(tradeCost) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance. Required: %s, Available: %s",
                            tradeCost, user.getBalance()));
        }
    }

    public void marketShareUpdate(Market market, Outcome outcome, int sharesToBuy) {
        market.applyTrade(outcome, sharesToBuy);
    }

    public void userBalanceUpdate(User user, BigDecimal tradeCost) {
        BigDecimal newBalance = user.getBalance().subtract(tradeCost);
        user.setBalance(newBalance);
    }

    public void updateUserPosition(User user, Market market, Outcome outcome, int sharesToBuy) {
        Position position = user.getOrCreatePosition(market.getMarketId());
        position.updatePosition(outcome, sharesToBuy);
    }

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
}
