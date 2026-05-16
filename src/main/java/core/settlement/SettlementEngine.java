package core.settlement;

import java.util.Collection;

import org.springframework.stereotype.Component;

import core.market.Market;
import core.market.MarketStatus;
import core.user.Position;
import core.user.User;

@Component
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

        // ======================== PHASE 2: APPLY MUTATIONS ========================
        // Balance is NOT mutated here — MarketService calls
        // LedgerService.recordSettlementCredit with the payout, which owns the
        // balance write. This engine only clears and marks the position.
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

}
