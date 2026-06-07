package core.settlement;

/**
 * Settlement failure that should be retried after the configured backoff.
 * Bumps {@code attempts} on the {@code position_settlements} row.
 */
public final class TransientSettlementException extends SettlementRowException {

    public TransientSettlementException(String marketId, String userId, String message, Throwable cause) {
        super(marketId, userId, message, cause);
    }
}
