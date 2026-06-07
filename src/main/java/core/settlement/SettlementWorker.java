package core.settlement;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls {@code market.position_settlements} and drives one settlement per row
 * through {@link SettlementProcessor}. Every API replica runs one instance;
 * concurrent workers don't collide because the processor's claim query uses
 * {@code FOR UPDATE SKIP LOCKED}.
 *
 * <p>See:
 * <ul>
 *   <li>{@code docs/adr/0002-async-settlement-via-postgres-queue.md}</li>
 *   <li>{@link SettlementProcessor} — per-row TX</li>
 *   <li>{@link SettlementBookkeeping} — failure / completion TXs</li>
 * </ul>
 *
 * <p>The worker itself is NOT transactional — each tick opens up to
 * {@code batchSize} independent transactions, one per row. A failure on one
 * row never rolls back work on another.
 *
 * <p>Default-on; flip {@code settlement.worker.enabled=false} to disable on a
 * given replica (e.g. while running integration tests against a shared DB).
 */
@Component
@ConditionalOnProperty(name = "settlement.worker.enabled", havingValue = "true", matchIfMissing = true)
public class SettlementWorker {

    private static final Logger log = LoggerFactory.getLogger(SettlementWorker.class);

    private final SettlementProcessor processor;
    private final SettlementBookkeeping bookkeeping;
    private final SettlementBackoff backoff;

    private final int batchSize;
    private final int maxAttempts;

    public SettlementWorker(SettlementProcessor processor,
            SettlementBookkeeping bookkeeping,
            @Value("${settlement.worker.batch-size:50}") int batchSize,
            @Value("${settlement.worker.max-attempts:5}") int maxAttempts,
            @Value("${settlement.worker.base-backoff-seconds:30}") long baseBackoffSeconds,
            @Value("${settlement.worker.jitter-seconds:10}") long jitterSeconds) {
        if (batchSize <= 0) throw new IllegalArgumentException("batch-size must be positive");
        if (maxAttempts <= 0) throw new IllegalArgumentException("max-attempts must be positive");
        this.processor = processor;
        this.bookkeeping = bookkeeping;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
        this.backoff = new SettlementBackoff(
                Duration.ofSeconds(baseBackoffSeconds),
                Duration.ofSeconds(jitterSeconds));
    }

    @Scheduled(fixedDelayString = "${settlement.worker.poll-ms:1000}")
    public void tick() {
        for (int i = 0; i < batchSize; i++) {
            if (!processOne()) return; // queue empty
        }
    }

    /**
     * @return true if work was attempted (continue this tick); false if the
     *         queue is empty or the claim itself failed and we should yield.
     */
    private boolean processOne() {
        try {
            SettlementProcessor.ProcessOutcome outcome = processor.claimAndProcessOne();
            if (outcome instanceof SettlementProcessor.Empty) {
                return false;
            }
            if (outcome instanceof SettlementProcessor.Settled s) {
                // Outside the per-row TX, in its own TX: try to finalize the Market.
                bookkeeping.maybeFlipResolved(s.marketId());
            }
            return true;
        } catch (TerminalSettlementException e) {
            // Fail fast: skip the retry budget.
            bookkeeping.recordFailure(e.getMarketId(), e.getUserId(),
                    describe(e), /* forceTerminal */ true, maxAttempts, backoff);
            return true;
        } catch (TransientSettlementException e) {
            bookkeeping.recordFailure(e.getMarketId(), e.getUserId(),
                    describe(e), /* forceTerminal */ false, maxAttempts, backoff);
            return true;
        } catch (RuntimeException e) {
            // Unclassified — claim query itself failed, DB blip, etc. Log and
            // back off this tick; the worker will retry on the next schedule.
            log.warn("Unclassified settlement worker error: {}", e.getMessage(), e);
            return false;
        }
    }

    private static String describe(SettlementRowException e) {
        Throwable cause = e.getCause();
        String causeMsg = cause == null ? "" : (" caused by " + cause.getClass().getSimpleName()
                + ": " + cause.getMessage());
        return e.getMessage() + causeMsg;
    }

    // Exposed for tests / operators.
    public int getMaxAttempts() { return maxAttempts; }
    public int getBatchSize() { return batchSize; }
    public SettlementBackoff getBackoff() { return backoff; }
}
