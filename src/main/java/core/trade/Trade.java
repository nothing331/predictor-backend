package core.trade;

import core.market.Outcome;
import java.math.BigDecimal;
import java.time.Instant;

public final class Trade {

    private final String userId;
    private final String marketId;
    private final Outcome outcome;
    private final double sharesBought;
    private final BigDecimal cost;

    public Trade(String userId2, String marketId2, Outcome outcome2, double sharesBought2, BigDecimal cost2) {
        this.userId = userId2;
        this.marketId = marketId2;
        this.outcome = outcome2;
        this.sharesBought = sharesBought2;
        this.cost = cost2;
    }

    public BigDecimal getCost() {
        return this.cost;
    }

}
