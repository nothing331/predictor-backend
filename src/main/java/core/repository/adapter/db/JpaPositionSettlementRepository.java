package core.repository.adapter.db;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import db.entity.PositionSettlementEntity;
import db.entity.PositionSettlementId;

@Repository
public interface JpaPositionSettlementRepository
        extends JpaRepository<PositionSettlementEntity, PositionSettlementId> {

    /**
     * Native query because Hibernate's HQL has no {@code SKIP LOCKED} syntax.
     * Returns at most one PENDING row whose {@code next_run_at <= now()}.
     * The {@code FOR UPDATE SKIP LOCKED} acquires a row lock visible only
     * inside the current transaction; concurrent workers see different rows.
     */
    @Query(value = """
            SELECT ps.market_id, ps.user_id, ps.status, ps.attempts, ps.last_error,
                   ps.next_run_at, ps.created_at, ps.updated_at
              FROM market.position_settlements ps
             WHERE ps.status = 'PENDING' AND ps.next_run_at <= now()
             ORDER BY ps.next_run_at
             FOR UPDATE SKIP LOCKED
             LIMIT 1
            """, nativeQuery = true)
    Optional<PositionSettlementEntity> claimNextPending();

    /**
     * Idempotent enqueue used by Resolution. We rely on the table PK
     * {@code (market_id, user_id)} for the conflict target.
     */
    @Modifying
    @Query(value = """
            INSERT INTO market.position_settlements (market_id, user_id)
            VALUES (:marketId, :userId)
            ON CONFLICT (market_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("marketId") String marketId, @Param("userId") String userId);

    /**
     * Bulk enqueue: one row per unsettled Position in this Market. Single
     * round-trip; idempotent via {@code ON CONFLICT DO NOTHING}.
     */
    @Modifying
    @Query(value = """
            INSERT INTO market.position_settlements (market_id, user_id)
            SELECT p.market_id, p.user_id
              FROM market.positions p
             WHERE p.market_id = :marketId
               AND p.settled = false
            ON CONFLICT (market_id, user_id) DO NOTHING
            """, nativeQuery = true)
    int enqueueUnsettledPositionsForMarket(@Param("marketId") String marketId);

    @Modifying
    @Query("delete from PositionSettlementEntity ps where ps.marketId = :marketId and ps.userId = :userId")
    int deleteByKey(@Param("marketId") String marketId, @Param("userId") String userId);

    @Modifying
    @Query("""
            update PositionSettlementEntity ps
               set ps.attempts = :attempts,
                   ps.lastError = :lastError,
                   ps.nextRunAt = :nextRunAt,
                   ps.updatedAt = :now
             where ps.marketId = :marketId
               and ps.userId = :userId
               and ps.status = 'PENDING'
            """)
    int updateForRetry(@Param("marketId") String marketId,
                       @Param("userId") String userId,
                       @Param("attempts") int attempts,
                       @Param("lastError") String lastError,
                       @Param("nextRunAt") Timestamp nextRunAt,
                       @Param("now") Timestamp now);

    @Modifying
    @Query("""
            update PositionSettlementEntity ps
               set ps.status = 'FAILED',
                   ps.attempts = :attempts,
                   ps.lastError = :lastError,
                   ps.updatedAt = :now
             where ps.marketId = :marketId
               and ps.userId = :userId
               and ps.status = 'PENDING'
            """)
    int markFailed(@Param("marketId") String marketId,
                   @Param("userId") String userId,
                   @Param("attempts") int attempts,
                   @Param("lastError") String lastError,
                   @Param("now") Timestamp now);

    @Query("""
            select case when count(ps) > 0 then true else false end
              from PositionSettlementEntity ps
             where ps.marketId = :marketId
            """)
    boolean existsByMarketId(@Param("marketId") String marketId);

    /**
     * Read-lock a PENDING row to drive the failure decision. Returns the
     * current {@code attempts} value, or empty if the row is missing / no longer
     * PENDING. Lock is released when the calling transaction ends. Native query
     * because HQL has no {@code FOR UPDATE} on scalar projections.
     */
    @Query(value = """
            SELECT attempts FROM market.position_settlements
             WHERE market_id = :marketId AND user_id = :userId AND status = 'PENDING'
             FOR UPDATE
            """, nativeQuery = true)
    java.util.Optional<Integer> lockAttemptsForFailure(@Param("marketId") String marketId,
                                                       @Param("userId") String userId);

    /**
     * Admin retry: flip every {@code FAILED} row for this Market back to
     * {@code PENDING} with cleared attempts/last_error and {@code next_run_at = now()}.
     * Idempotent: subsequent calls match zero rows.
     */
    @Modifying
    @Query("""
            update PositionSettlementEntity ps
               set ps.status = 'PENDING',
                   ps.attempts = 0,
                   ps.lastError = null,
                   ps.nextRunAt = :now,
                   ps.updatedAt = :now
             where ps.marketId = :marketId
               and ps.status = 'FAILED'
            """)
    int resetFailedRowsForRetry(@Param("marketId") String marketId,
                                @Param("now") Timestamp now);
}
