package core.settlement;

/**
 * Settlement failure that should NOT be retried — a logical impossibility
 * (FK violation, missing position, missing outcome) that no amount of
 * retrying will fix. Causes the row to be marked {@code FAILED} immediately
 * and the Market to transition to {@code SETTLEMENT_FAILED}.
 */
public final class TerminalSettlementException extends SettlementRowException {

    public TerminalSettlementException(String marketId, String userId, String message, Throwable cause) {
        super(marketId, userId, message, cause);
    }
}
