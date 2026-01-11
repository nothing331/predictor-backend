package core.user;

/**
 * Represents a user's position (share holdings) in a specific market.
 * 
 * This is a simple data container with no business logic.
 * Positions track how many YES and NO shares a user owns in a given market.
 * 
 * @author Prediction Market Team
 */
public class Position {

    private final String marketId;
    private double yesShares;
    private double noShares;

    public Position(String marketId) {
        this.marketId = marketId;
        this.yesShares = 0.0;
        this.noShares = 0.0;
    }

    public Position(String marketId, double yesShares, double noShares) {
        this.marketId = marketId;
        this.yesShares = yesShares;
        this.noShares = noShares;
    }

    // ======================== GETTERS ========================

    public String getMarketId() {
        return marketId;
    }

    public double getYesShares() {
        return yesShares;
    }

    public double getNoShares() {
        return noShares;
    }

    public void setYesShares(double yesShares) {
        if (yesShares < 0) {
            throw new IllegalArgumentException("yesShares cannot be negative");
        }
        this.yesShares = yesShares;
    }

    public void setNoShares(double noShares) {
        if (noShares < 0) {
            throw new IllegalArgumentException("noShares cannot be negative");
        }
        this.noShares = noShares;
    }
}
