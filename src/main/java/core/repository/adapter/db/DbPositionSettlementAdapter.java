package core.repository.adapter.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import core.repository.port.PositionSettlementRepository;
import core.settlement.PositionSettlement;
import db.entity.PositionSettlementEntity;

@Repository
public class DbPositionSettlementAdapter implements PositionSettlementRepository {

    private final JpaPositionSettlementRepository jpa;

    public DbPositionSettlementAdapter(JpaPositionSettlementRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void enqueueAll(List<PositionSettlement> rows) {
        if (rows == null || rows.isEmpty()) return;
        for (PositionSettlement row : rows) {
            jpa.insertIfAbsent(row.getMarketId(), row.getUserId());
        }
    }

    @Override
    public int enqueueUnsettledPositionsForMarket(String marketId) {
        return jpa.enqueueUnsettledPositionsForMarket(marketId);
    }

    @Override
    public Optional<PositionSettlement> claimNextPending() {
        return jpa.claimNextPending().map(this::toDomain);
    }

    @Override
    public boolean deleteByKey(String marketId, String userId) {
        return jpa.deleteByKey(marketId, userId) > 0;
    }

    @Override
    public boolean updateForRetry(String marketId, String userId, int attempts,
                                  String lastError, Instant nextRunAt) {
        Timestamp now = Timestamp.from(Instant.now());
        return jpa.updateForRetry(marketId, userId, attempts, truncate(lastError),
                Timestamp.from(nextRunAt), now) > 0;
    }

    @Override
    public boolean markFailed(String marketId, String userId, int attempts, String lastError) {
        Timestamp now = Timestamp.from(Instant.now());
        return jpa.markFailed(marketId, userId, attempts, truncate(lastError), now) > 0;
    }

    @Override
    public boolean existsByMarketId(String marketId) {
        return jpa.existsByMarketId(marketId);
    }

    @Override
    public java.util.Optional<Integer> lockAttemptsForFailure(String marketId, String userId) {
        return jpa.lockAttemptsForFailure(marketId, userId);
    }

    @Override
    public int resetFailedRowsForRetry(String marketId) {
        Timestamp now = Timestamp.from(Instant.now());
        return jpa.resetFailedRowsForRetry(marketId, now);
    }

    private PositionSettlement toDomain(PositionSettlementEntity e) {
        return new PositionSettlement(
                e.getMarketId(),
                e.getUserId(),
                e.getStatus(),
                e.getAttempts(),
                e.getLastError(),
                e.getNextRunAt() != null ? e.getNextRunAt().toInstant() : null,
                e.getCreatedAt() != null ? e.getCreatedAt().toInstant() : null,
                e.getUpdatedAt() != null ? e.getUpdatedAt().toInstant() : null);
    }

    /**
     * Defensive truncation for {@code last_error}. The column is TEXT (unlimited)
     * but a stack trace can dwarf a row; keep the most actionable prefix.
     */
    private String truncate(String s) {
        if (s == null) return null;
        return s.length() <= 4000 ? s : s.substring(0, 4000);
    }
}
