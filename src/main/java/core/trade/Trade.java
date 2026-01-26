package core.trade;

import core.market.Outcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Trade {

    private final String userId;
    private final String marketId;
    private final Outcome outcome;
    private final double sharesBought;
    private final BigDecimal cost;
    private final Instant createdAt;
    private final String tradeId;

    public Trade(String userId2, String marketId2, Outcome outcome2, double sharesBought2, BigDecimal cost2) {
        this.userId = userId2;
        this.marketId = marketId2;
        this.outcome = outcome2;
        this.sharesBought = sharesBought2;
        this.cost = cost2;
        this.createdAt = Instant.now();
        this.tradeId = UUID.randomUUID().toString();
    }

    public BigDecimal getCost() {
        return this.cost;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getMarketId() {
        return this.marketId;
    }

    public double getShareCount() {
        return this.sharesBought;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }
}
