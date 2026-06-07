package core.settlement;

/**
 * Base class for failures during per-row settlement. Carries the row identity
 * so the worker can route bookkeeping after the transaction rolls back.
 */
public abstract sealed class SettlementRowException extends RuntimeException
        permits TransientSettlementException, TerminalSettlementException {

    private final String marketId;
    private final String userId;

    protected SettlementRowException(String marketId, String userId, String message, Throwable cause) {
        super(message, cause);
        this.marketId = marketId;
        this.userId = userId;
    }

    public String getMarketId() { return marketId; }
    public String getUserId() { return userId; }
}
