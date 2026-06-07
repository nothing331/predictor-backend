package core.settlement;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes the next retry time for a failed settlement attempt.
 *
 * <p>Schedule: {@code next = base * 2^attempts + uniform(0, jitter)}.
 * With base=30s, jitter=10s, attempts ∈ {1..5} retries land at roughly
 * 30s, 1m, 2m, 4m, 8m — total budget ≈ 15 minutes.
 *
 * <p>See {@code docs/adr/0002-async-settlement-via-postgres-queue.md} (retry section).
 */
public final class SettlementBackoff {

    private final Duration base;
    private final Duration jitter;

    public SettlementBackoff(Duration base, Duration jitter) {
        if (base == null || base.isNegative() || base.isZero()) {
            throw new IllegalArgumentException("base must be positive");
        }
        if (jitter == null || jitter.isNegative()) {
            throw new IllegalArgumentException("jitter must be non-negative");
        }
        this.base = base;
        this.jitter = jitter;
    }

    /**
     * @param attemptsAlreadyTaken the {@code attempts} value AFTER incrementing —
     *        i.e. how many tries have now been made.
     */
    public Instant nextRunAt(Instant now, int attemptsAlreadyTaken) {
        // First retry (attemptsAlreadyTaken == 1) waits one full `base`, so the
        // schedule is base * 2^(attempts-1): 30s, 1m, 2m, 4m, 8m for base=30s.
        long shift = Math.min(Math.max(attemptsAlreadyTaken - 1, 0), 30); // guard against overflow
        long baseSeconds = base.getSeconds() << shift;
        long jitterSeconds = jitter.isZero() ? 0 : ThreadLocalRandom.current().nextLong(jitter.getSeconds() + 1);
        return now.plusSeconds(baseSeconds + jitterSeconds);
    }
}
