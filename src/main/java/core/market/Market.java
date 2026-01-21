package core.market;

import java.math.BigDecimal;
import core.lmsr.PricingEngine;

public class Market {
    private String marketId;
    private String marketName;
    private String marketDescription;
    private double qYes; // Total YES shares in the market pool
    private double qNo; // Total NO shares in the market pool
    private double liquidity; // Liquidity parameter (b) for LMSR
    private MarketStatus status;
    private Outcome resolvedOutcome;

    public Market(String marketId, String marketName, String marketDescription) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.marketDescription = marketDescription;
        this.qYes = 0.0;
        this.qNo = 0.0;
        this.liquidity = 100.0;
        this.status = MarketStatus.OPEN;
        this.resolvedOutcome = null;
    }

    public MarketStatus getStatus() {
        return this.status;
    }

    public double getLiquidity() {
        return this.liquidity;
    }

    public String getMarketId() {
        return this.marketId;
    }

    public double getQYes() {
        return this.qYes;
    }

    public double getQNo() {
        return this.qNo;
    }

    public double getYesPrice() {
        return PricingEngine.displayYesPrice(this.qYes, this.qNo, this.liquidity);
    }

    public Outcome getResolvedOutcome() {
        return this.resolvedOutcome;
    }

    /**
     * Get the current NO price/probability.
     * This is computed dynamically from the cost function derivative.
     * 
     * @return Price between 0.0 and 1.0
     */
    public double getNoPrice() {
        return PricingEngine.displayNoPrice(this.qYes, this.qNo, this.liquidity);
    }

    // ======================== TRADING (Week 1: Buy Only) ========================

    /**
     * Calculate the cost to buy shares of a given outcome.
     */
    public BigDecimal getCostToBuy(Outcome outcome, double shares) {
        if (outcome == Outcome.YES) {

            return PricingEngine.calculateYesPrice(this.qYes, this.qNo, this.liquidity, shares);
        } else {
            return PricingEngine.calculateNoPrice(this.qYes, this.qNo, this.liquidity, shares);
        }
    }

    /**
     * Set the YES share count atomically.
     * Used by TradeEngine to enforce atomicity.
     */
    public void setQYes(double qYes) {
        if (this.status != MarketStatus.OPEN) {
            throw new IllegalStateException("Cannot modify shares when market is not open");
        }
        if (qYes < 0) {
            throw new IllegalArgumentException("qYes cannot be negative");
        }
        this.qYes = qYes;
    }

    /**
     * Set the NO share count atomically.
     * Used by TradeEngine to enforce atomicity.
     */
    public void setQNo(double qNo) {
        if (this.status != MarketStatus.OPEN) {
            throw new IllegalStateException("Cannot modify shares when market is not open");
        }
        if (qNo < 0) {
            throw new IllegalArgumentException("qNo cannot be negative");
        }
        this.qNo = qNo;
    }

    public void applyTrade(Outcome outcome, double shares) {
        if (this.status != MarketStatus.OPEN) {
            throw new IllegalStateException("Cannot trade on non-open market");

        }

        if (shares <= 0) {
            throw new IllegalArgumentException("Shares must be positive");
        }

        if (outcome == Outcome.YES) {
            this.qYes += shares;
        } else {
            this.qNo += shares;
        }
    }

    // ? on expansion a DB check is still needed
    public synchronized void resolveMarket(Outcome outcome) {
        if (outcome == null)
            throw new IllegalArgumentException("Outcome cannot be null");
        if (status == MarketStatus.RESOLVED)
            throw new IllegalStateException("Market already resolved");
        if (status != MarketStatus.OPEN) {
            throw new IllegalStateException("Only OPEN markets can be resolved");
        }

        resolvedOutcome = outcome;
        status = MarketStatus.RESOLVED;
    }
}
