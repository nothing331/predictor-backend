package core.settlement;

import java.math.BigDecimal;
import java.util.Collection;

import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.user.Position;
import core.user.User;

public class SettlementEngine {

    public void settleMarket(Market market, Collection<User> users) {
        validateMarketResolved(market);

        for (User user : users) {
            Position position = user.getPosition(market.getMarketId());

            if (position == null) {
                continue;
            }

            if (position.isSettled()) {
                continue;
            }

            settleUser(user, market);
        }
        // Note: Caller must persist state (Markets, Users) after settlement completes.
    }

    public void settleUser(User user, Market market) {
        validateMarketResolved(market);

        Position position = user.getPosition(market.getMarketId());
        if (position == null) {
            throw new IllegalArgumentException(
                    "User " + user.getUserId() + " has no position in market " + market.getMarketId());
        }

        if (position.isSettled()) {
            throw new IllegalStateException(
                    "Position for user " + user.getUserId() + " in market " + market.getMarketId()
                            + " already settled");
        }

        // ======================== PHASE 2: COMPUTE PAYOUT ========================
        Outcome winningOutcome = market.getResolvedOutcome();
        double winningShares = getWinningShares(position, winningOutcome);

        // Payout = winning shares × 1 (each winning share pays out 1 unit)
        BigDecimal payout = BigDecimal.valueOf(winningShares);
        BigDecimal newBalance = user.getBalance().add(payout);

        // ======================== PHASE 3: APPLY MUTATIONS ========================
        // All validations passed, now we mutate state atomically

        // 1. Update user balance
        user.setBalance(newBalance);

        // 2. Clear and mark position as settled
        position.clearShares();
        position.markAsSettled();
    }

    // ======================== PRIVATE HELPERS ========================

    /**
     * Validate that the market is in RESOLVED state.
     */
    private void validateMarketResolved(Market market) {
        if (market == null) {
            throw new IllegalArgumentException("Market cannot be null");
        }
        if (market.getStatus() != MarketStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Market " + market.getMarketId() + " must be resolved before settlement. Current status: "
                            + market.getStatus());
        }
        if (market.getResolvedOutcome() == null) {
            throw new IllegalStateException(
                    "Market " + market.getMarketId() + " is resolved but has no resolved outcome");
        }
    }

    private double getWinningShares(Position position, Outcome winningOutcome) {
        if (winningOutcome == Outcome.YES) {
            return position.getYesShares();
        } else {
            return position.getNoShares();
        }
    }
}
