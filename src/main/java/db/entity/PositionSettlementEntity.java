package db.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * JPA mapping for the {@code market.position_settlements} work queue (Flyway V11).
 *
 * <p>See:
 * <ul>
 *   <li>{@code docs/adr/0002-async-settlement-via-postgres-queue.md}</li>
 *   <li>{@code src/main/resources/db/migration/V11__create_position_settlements.sql}</li>
 * </ul>
 *
 * <p>Status is one of {@code "PENDING"} or {@code "FAILED"} (CHECK-constrained at the DB).
 * {@code "DONE"} is intentionally not a stored state — successful settlement deletes the row.
 */
@Entity
@Table(name = "position_settlements", schema = "market")
@IdClass(PositionSettlementId.class)
public class PositionSettlementEntity {

    @Id
    @Column(name = "market_id", nullable = false, length = 36)
    private String marketId;

    @Id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "next_run_at", nullable = false)
    private Timestamp nextRunAt;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    protected PositionSettlementEntity() {
    }

    public PositionSettlementEntity(String marketId, String userId, String status, int attempts,
            String lastError, Timestamp nextRunAt, Timestamp createdAt, Timestamp updatedAt) {
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
    public Timestamp getNextRunAt() { return nextRunAt; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public void setStatus(String status) { this.status = status; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public void setNextRunAt(Timestamp nextRunAt) { this.nextRunAt = nextRunAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
