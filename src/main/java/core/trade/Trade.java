package core.trade;

import core.market.Outcome;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Trade {

    private String userId;
    private String marketId;
    private Outcome outcome;
    private double sharesBought;
    private BigDecimal cost;
    private Instant createdAt;
    private String tradeId;

    /**
     * Default constructor for frameworks (e.g., Jackson).
     */
    protected Trade() {
    }

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

    public Outcome getOutcome() {
        return this.outcome;
    }

    public String getTradeId() {
        return this.tradeId;
    }
}
