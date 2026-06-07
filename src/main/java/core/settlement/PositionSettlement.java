package core.settlement;

import java.time.Instant;

/**
 * Domain representation of one row in the {@code market.position_settlements}
 * work queue. Identified by the (marketId, userId) composite key.
 *
 * <p>{@link #STATUS_PENDING} = waiting to be processed (or retried).
 * {@link #STATUS_FAILED} = terminal failure; requires admin retry.
 * {@code DONE} is intentionally not a status — successful settlement deletes
 * the row. See {@code docs/adr/0002-async-settlement-via-postgres-queue.md}.
 */
public class PositionSettlement {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED  = "FAILED";

    private final String marketId;
    private final String userId;
    private final String status;
    private final int attempts;
    private final String lastError;
    private final Instant nextRunAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PositionSettlement(String marketId, String userId, String status, int attempts,
            String lastError, Instant nextRunAt, Instant createdAt, Instant updatedAt) {
        this.marketId = marketId;
        this.userId = userId;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.nextRunAt = nextRunAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getMarketId() { return marketId; }
    public String getUserId() { return userId; }
    public String getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getNextRunAt() { return nextRunAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
