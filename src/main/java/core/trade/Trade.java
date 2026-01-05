package core.trade;

import core.market.Outcome;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable value object representing a completed trade.
 * 
 * A Trade captures WHAT HAPPENED, not HOW it happened.
 * Once created, a Trade cannot be modified.
 * 
 * Fields:
 * - userId: Who made the trade
 * - marketId: Which market was traded
 * - outcome: YES or NO
 * - sharesBought: Number of shares acquired
 * - cost: Amount paid (BigDecimal for precision)
 * - timestamp: When the trade occurred
 * 
 * @author Prediction Market Team
 */
public final class Trade {

    private final String userId;
    private final String marketId;
    private final Outcome outcome;
    private final double sharesBought;
    private final BigDecimal cost;

    public Trade(String userId2, String marketId2, Outcome outcome2, double sharesBought2, BigDecimal cost2,
            Instant now) {
        this.userId = userId2;
        this.marketId = marketId2;
        this.outcome = outcome2;
        this.sharesBought = sharesBought2;
        this.cost = cost2;
    }

}
