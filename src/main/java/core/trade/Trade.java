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
    private String clientRequestId;

    /**
     * Default constructor for frameworks (e.g., Jackson).
     */
    protected Trade() {
    }

    public Trade(String userId, String marketId, Outcome outcome, double sharesBought, BigDecimal cost) {
        this(UUID.randomUUID().toString(), userId, marketId, outcome, sharesBought, cost, Instant.now(), null);
    }

    public Trade(String userId, String marketId, Outcome outcome, double sharesBought, BigDecimal cost,
            String clientRequestId) {
        this(UUID.randomUUID().toString(), userId, marketId, outcome, sharesBought, cost, Instant.now(),
                clientRequestId);
    }

    public Trade(String tradeId, String userId, String marketId, Outcome outcome, double sharesBought, BigDecimal cost,
            Instant createdAt) {
        this(tradeId, userId, marketId, outcome, sharesBought, cost, createdAt, null);
    }

    public Trade(String tradeId, String userId, String marketId, Outcome outcome, double sharesBought, BigDecimal cost,
            Instant createdAt, String clientRequestId) {
        this.tradeId = tradeId;
        this.userId = userId;
        this.marketId = marketId;
        this.outcome = outcome;
        this.sharesBought = sharesBought;
        this.cost = cost;
        this.createdAt = createdAt;
        this.clientRequestId = clientRequestId;
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

    public String getClientRequestId() {
        return clientRequestId;
    }
}
