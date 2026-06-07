package core.repository.port;

import java.util.Collection;
import core.market.Market;

public interface MarketRepository {
    void saveAll(Collection<Market> markets);

    Collection<Market> loadAll();

    Market loadById(String marketId);

    Collection<Market> loadByStatus(String status);

    /**
     * Fetch a Market row with {@code SELECT ... FOR UPDATE}. Used by Resolution
     * to serialize against concurrent buys (which take {@code FOR SHARE}).
     * Lock order across the codebase: {@code Market -> User}.
     * See docs/adr/0004-lock-order-market-then-user.md.
     */
    Market loadByIdForUpdate(String marketId);

    /**
     * Fetch a Market row with {@code SELECT ... FOR SHARE}. Used by buys to coexist
     * with other concurrent buys while still blocking against Resolution's
     * {@code FOR UPDATE}. Must be acquired BEFORE the {@code FOR UPDATE} on the
     * User row to preserve the {@code Market -> User} lock order.
     * See docs/adr/0004-lock-order-market-then-user.md.
     */
    Market loadByIdForShare(String marketId);

    /**
     * Atomically flip the Market to {@code RESOLVED} iff every Position has been
     * settled. The {@code NOT EXISTS} subquery against {@code position_settlements}
     * is evaluated under a row-level lock acquired by the {@code UPDATE} itself,
     * so concurrent workers cannot both observe "no remaining rows" and double-flip.
     * Idempotent: if the Market is no longer in {@code RESOLUTION_PENDING} the
     * WHERE clause matches zero rows.
     *
     * <p>Does NOT update {@code resolved_at} — that timestamp was set at the
     * {@code OPEN -> RESOLUTION_PENDING} transition and reflects when the
     * outcome was decided.
     *
     * @return true if this call flipped the Market; false if it was already done.
     */
    boolean markResolvedIfFullySettled(String marketId);

    /**
     * Atomically flip the Market to {@code SETTLEMENT_FAILED}. Called when a
     * {@code position_settlements} row terminally fails. Idempotent: if the
     * Market is not in {@code RESOLUTION_PENDING} the WHERE clause matches
     * zero rows.
     *
     * @return true if this call flipped the Market; false if it was already done.
     */
    boolean markSettlementFailed(String marketId);

    /**
     * Admin retry: flip {@code SETTLEMENT_FAILED -> RESOLUTION_PENDING} so the
     * Settlement Worker can pick the rows up again. Idempotent: no-op for any
     * Market not in {@code SETTLEMENT_FAILED}.
     *
     * @return true if this call flipped the Market; false otherwise.
     */
    boolean flipBackToResolutionPending(String marketId);
}
