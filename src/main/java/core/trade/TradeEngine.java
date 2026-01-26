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

    public Trade executeTrade(User user, Market market, Outcome outcome, double sharesToBuy) {
        // Input validation
        if (sharesToBuy <= 0.0) {
            throw new IllegalArgumentException("Shares to buy must be positive, got: " + sharesToBuy);
        }

        // ========== PHASE 1: COMPUTE ALL NEW VALUES (Pure Reads, No Mutations)
        // ==========

        // Calculate trade cost
        BigDecimal tradeCost = market.getCostToBuy(outcome, sharesToBuy);

        // Validate before ANY mutations
        validateTrade(user, market, tradeCost);

        // Compute new market share counts
        double currentQYes = market.getQYes();
        double currentQNo = market.getQNo();
        double newQYes = currentQYes;
        double newQNo = currentQNo;

        if (outcome == Outcome.YES) {
            newQYes = currentQYes + sharesToBuy;
        } else {
            newQNo = currentQNo + sharesToBuy;
        }

        // Compute new user balance
        BigDecimal newBalance = user.getBalance().subtract(tradeCost);

        // Compute new position shares
        // NOTE: We do NOT use getOrCreatePosition() here to avoid mutating user state
        // in Phase 1
        Position existingPosition = user.getPosition(market.getMarketId());
        double currentYesShares = (existingPosition != null) ? existingPosition.getYesShares() : 0.0;
        double currentNoShares = (existingPosition != null) ? existingPosition.getNoShares() : 0.0;

        double newYesShares = currentYesShares;
        double newNoShares = currentNoShares;

        if (outcome == Outcome.YES) {
            newYesShares = currentYesShares + sharesToBuy;
        } else {
            newNoShares = currentNoShares + sharesToBuy;
        }

        // ========== PHASE 2: APPLY ALL MUTATIONS ATOMICALLY ==========
        // All computations succeeded, now apply all state changes together

        // Update market shares
        if (outcome == Outcome.YES) {
            market.setQYes(newQYes);
        } else {
            market.setQNo(newQNo);
        }

        // Update user balance
        user.setBalance(newBalance);

        // Update user position
        // NOW it is safe to create the position if it doesn't exist
        Position position = user.getOrCreatePosition(market.getMarketId());
        if (outcome == Outcome.YES) {
            position.setYesShares(newYesShares);
        } else {
            position.setNoShares(newNoShares);
        }

        // ========== CREATE TRADE RECORD ==========
        return new Trade(
                user.getUserId(),
                market.getMarketId(),
                outcome,
                sharesToBuy,
                tradeCost);
        // Note: The caller (PredictionMarketGame) is responsible for persisting
        // the state (Market, User, Trade) immediately after this method returns.
    }

    public BigDecimal getTradeCost(Market market, Outcome outcome, int sharesToBuy) {
        return market.getCostToBuy(outcome, sharesToBuy);
    }

    public void validateTrade(User user, Market market, BigDecimal tradeCost) {
        // Guard: Cost cannot be negative
        if (tradeCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Trade cost cannot be negative");
        }

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

    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }
}
