package core.market;

import core.lmsr.PricingEngine;

/**
 * Represents a prediction market using LMSR pricing.
 */
public class Market {
    private int marketId;
    private String marketName;
    private String marketDescription;
    private double qYes; // Total YES shares in the market pool
    private double qNo; // Total NO shares in the market pool
    private double liquidity; // Liquidity parameter (b) for LMSR
    private MarketStatus status;
    private Outcome resolvedOutcome;

    public Market(int marketId, String marketName, String marketDescription) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.marketDescription = marketDescription;
        this.qYes = 0.0;
        this.qNo = 0.0;
        this.liquidity = 100.0;
        this.status = MarketStatus.OPEN;
        this.resolvedOutcome = null;
    }

    /**
     * Get the current YES price/probability.
     * This is computed dynamically from the cost function derivative.
     * 
     * @return Price between 0.0 and 1.0
     */
    public double getYesPrice() {
        return PricingEngine.displayYesPrice(this.qYes, this.qNo, this.liquidity);
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
    public double getCostToBuy(Outcome outcome, double shares) {
        if (outcome == Outcome.YES) {

            return PricingEngine.calculateYesPrice(this.qYes, this.qNo, this.liquidity, shares);
        } else {
            return PricingEngine.calculateNoPrice(this.qYes, this.qNo, this.liquidity, shares);
        }
    }

    /**
     * Apply a buy trade to the market.
     */
    public void applyTrade(Outcome outcome, double shares) {
        if (this.status == MarketStatus.OPEN) {

            if (shares <= 0) {
                throw new IllegalArgumentException("Shares must be positive");
            }

            // Buying increases the share count in the pool
            if (outcome == Outcome.YES) {
                this.qYes += shares;
            } else {
                this.qNo += shares;
            }
        }
        // NOTE: No price update needed - prices are computed on demand
    }

    public void resolveMarket(Outcome outcome) {
        if (this.status != MarketStatus.RESOLVED) {
            this.resolvedOutcome = outcome;
            this.status = MarketStatus.RESOLVED;
        }
    }
}
