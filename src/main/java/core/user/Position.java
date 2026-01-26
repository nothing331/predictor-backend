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
    private boolean settled;
    private String userId;

    public Position(String marketId) {
        this.marketId = marketId;
        this.yesShares = 0.0;
        this.noShares = 0.0;
        this.settled = false;
    }

    public Position(String marketId, double yesShares, double noShares, String userId) {
        this.marketId = marketId;
        this.yesShares = yesShares;
        this.noShares = noShares;
        this.settled = false;
        this.userId = userId;
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

    // ======================== SETTLEMENT ========================

    public boolean isSettled() {
        return settled;
    }

    /**
     * Mark this position as settled.
     * Once settled, the position cannot be settled again.
     */
    public void markAsSettled() {
        if (this.settled) {
            throw new IllegalStateException("Position already settled");
        }
        this.settled = true;
    }

    /**
     * Clear all shares from this position (used during settlement).
     * Sets both YES and NO shares to 0.
     */
    public void clearShares() {
        this.yesShares = 0.0;
        this.noShares = 0.0;
    }
}
