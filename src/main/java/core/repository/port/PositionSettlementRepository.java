package core.repository.port;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import core.settlement.PositionSettlement;

/**
 * Port for the {@code market.position_settlements} work queue.
 * Implementations must enforce the locking semantics promised by each method.
 * See {@code docs/adr/0002-async-settlement-via-postgres-queue.md}.
 */
public interface PositionSettlementRepository {

    /**
     * Enqueue rows for a Market's Positions during Resolution. Uses
     * {@code INSERT ... ON CONFLICT DO NOTHING} so re-running Resolution after
     * a partial commit is idempotent. Runs in the caller's transaction.
     */
    void enqueueAll(List<PositionSettlement> rows);

    /**
     * Enqueue one {@code position_settlements} row for every unsettled Position
     * in this Market. Single SQL statement of the form
     * {@code INSERT ... SELECT ... ON CONFLICT DO NOTHING}; safe to re-run.
     * Runs in the caller's transaction — the row inserts must commit atomically
     * with the {@code OPEN -> RESOLUTION_PENDING} status flip.
     *
     * @return the number of rows inserted (0 if all already existed).
     */
    int enqueueUnsettledPositionsForMarket(String marketId);

    /**
     * Claim the next {@code PENDING} row whose {@code next_run_at <= now()} via
     * {@code SELECT ... FOR UPDATE SKIP LOCKED LIMIT 1}. Returns empty if no
     * eligible row is visible. Must be invoked inside a transaction; the row
     * lock is released when that transaction commits or rolls back.
     */
    Optional<PositionSettlement> claimNextPending();

    /**
     * Delete the row for {@code (marketId, userId)}. Used on successful
     * settlement. Runs in the caller's transaction.
     *
     * @return true if a row was deleted, false if no row existed.
     */
    boolean deleteByKey(String marketId, String userId);

    /**
     * Update a PENDING row with new retry bookkeeping after a transient
     * failure. Match clause includes {@code status = 'PENDING'} so a row that
     * concurrently became FAILED or was deleted is left alone.
     *
     * @return true if a row was updated, false if none matched.
     */
    boolean updateForRetry(String marketId, String userId, int attempts,
                           String lastError, Instant nextRunAt);

    /**
     * Mark a row terminally FAILED. Match clause includes
     * {@code status = 'PENDING'} so the transition is one-way.
     *
     * @return true if a row was updated, false if none matched.
     */
    boolean markFailed(String marketId, String userId, int attempts, String lastError);

    /**
     * @return true if any row (PENDING or FAILED) still exists for the Market.
     *         Used by the "is the Market fully settled?" check.
     */
    boolean existsByMarketId(String marketId);

    /**
     * Lock a PENDING row {@code FOR UPDATE} and return its current {@code attempts}.
     * Used by {@link core.settlement.SettlementBookkeeping} to decide whether the
     * next failure should mark the row terminally FAILED. Returns empty when the
     * row has already been deleted or marked FAILED concurrently.
     */
    java.util.Optional<Integer> lockAttemptsForFailure(String marketId, String userId);

    /**
     * Admin retry: flip every {@code FAILED} row for this Market back to
     * {@code PENDING}, reset attempts to 0, clear {@code last_error}, set
     * {@code next_run_at = now()}. Worker picks them up on the next tick.
     *
     * @return number of rows reset.
     */
    int resetFailedRowsForRetry(String marketId);
}
