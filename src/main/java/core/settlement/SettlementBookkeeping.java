package core.settlement;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import core.event.MarketSettlementCompletedEvent;
import core.repository.port.MarketRepository;
import core.repository.port.PositionSettlementRepository;

/**
 * Post-processing transactions invoked by {@link SettlementWorker} after the
 * per-row {@link SettlementProcessor} transaction has committed or rolled back.
 *
 * <p>Each method runs in its OWN transaction ({@code REQUIRES_NEW}) so a
 * rolled-back per-row TX can still durably record its attempt count, last
 * error, and (for terminal failures) the Market transition.
 *
 * <p>See {@code docs/adr/0002-async-settlement-via-postgres-queue.md}.
 */
@Service
public class SettlementBookkeeping {

    private static final Logger log = LoggerFactory.getLogger(SettlementBookkeeping.class);

    private final PositionSettlementRepository positionSettlementRepository;
    private final MarketRepository marketRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementBookkeeping(PositionSettlementRepository positionSettlementRepository,
            MarketRepository marketRepository, ApplicationEventPublisher eventPublisher) {
        this.positionSettlementRepository = positionSettlementRepository;
        this.marketRepository = marketRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Record a failure on the {@code (marketId, userId)} row.
     *
     * <p>Reads {@code attempts} under {@code FOR UPDATE}, decides whether the
     * next try would exceed {@code maxAttempts}, and either:
     * <ul>
     *   <li>writes the new {@code attempts}, {@code last_error}, and
     *       {@code next_run_at} (transient retry), or</li>
     *   <li>marks the row {@code FAILED} and flips the Market to
     *       {@code SETTLEMENT_FAILED} (terminal).</li>
     * </ul>
     *
     * <p>{@code forceTerminal=true} skips the retry budget entirely — used for
     * logical impossibilities (FK violation, missing Position, etc.) signaled
     * by {@link TerminalSettlementException}.
     *
     * <p>Idempotent against concurrent completion: if the row is gone or
     * already {@code FAILED}, this method no-ops.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String marketId, String userId, String cause,
                              boolean forceTerminal, int maxAttempts, SettlementBackoff backoff) {
        Optional<Integer> currentAttempts =
                positionSettlementRepository.lockAttemptsForFailure(marketId, userId);
        if (currentAttempts.isEmpty()) {
            // Row was concurrently completed (deleted) or already FAILED.
            log.debug("recordFailure: no PENDING row for ({}, {})", marketId, userId);
            return;
        }
        int newAttempts = currentAttempts.get() + 1;
        boolean terminal = forceTerminal || newAttempts >= maxAttempts;

        if (terminal) {
            positionSettlementRepository.markFailed(marketId, userId, newAttempts, cause);
            boolean marketFlipped = marketRepository.markSettlementFailed(marketId);
            log.warn("Settlement terminally failed for ({}, {}): {} (marketFlipped={})",
                    marketId, userId, cause, marketFlipped);
        } else {
            Instant nextRunAt = backoff.nextRunAt(Instant.now(), newAttempts);
            positionSettlementRepository.updateForRetry(marketId, userId, newAttempts, cause, nextRunAt);
            log.info("Settlement transient failure for ({}, {}) attempt {}/{}: {}",
                    marketId, userId, newAttempts, maxAttempts, cause);
        }
    }

    /**
     * Called after every successful row processing. Atomically flips the Market
     * to {@code RESOLVED} if no {@code position_settlements} rows remain.
     * Idempotent: if rows still remain (PENDING or FAILED) the WHERE clause
     * matches zero rows and nothing happens.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void maybeFlipResolved(String marketId) {
        boolean flipped = marketRepository.markResolvedIfFullySettled(marketId);
        if (flipped) {
            log.info("Market {} fully settled, flipped to RESOLVED", marketId);
            // Fires AFTER the flip TX commits (TransactionalEventListener
            // contract) so cache invalidation sees the new RESOLVED status.
            eventPublisher.publishEvent(new MarketSettlementCompletedEvent(marketId));
        }
    }
}
