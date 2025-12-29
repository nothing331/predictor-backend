package core.market;

import core.lmsr.PricingEngine;

public class Market {
    private int marketId;
    private String marketName;
    private String marketDescription;
    private double yesShares;
    private double noShares;
    private double yesPrice;
    private double noPrice;
    private double liquidity;
    private boolean status;
    private Outcome resolvedOutcome;

    public Market(int marketId, String marketName, String marketDescription, boolean resolvedOutcome) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.marketDescription = marketDescription;
        this.yesShares = 0.0;
        this.noShares = 0.0;
        this.liquidity = 100.0;
        this.status = false;
        this.resolvedOutcome = null;
        this.yesPrice = 0.0;
        this.noPrice = 0.0;
    }

    public void getPrices() {
        this.yesPrice = PricingEngine.displayYesPrice(this.yesShares, this.noShares, this.liquidity);
        this.noPrice = PricingEngine.displayNoPrice(this.yesShares, this.noShares, this.liquidity);
    }

    public void applyTrade(Outcome outcome, double shares) {
        if (outcome == Outcome.YES) {
            this.yesShares = PricingEngine.sellYesPrice(this.yesShares, this.noShares, this.liquidity, shares);
        } else {
            this.noShares = PricingEngine.sellNoPrice(this.yesShares, this.noShares, this.liquidity, shares);
        }
        this.getPrices();
    }

    public void resolveMarket(Outcome outcome) {
        this.resolvedOutcome = outcome;
        this.status = true;
    }
}
