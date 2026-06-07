package core.repository.adapter.db;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import db.entity.MarketEntity;
import jakarta.persistence.LockModeType;

@Repository
public interface JpaMarketRepository extends JpaRepository<MarketEntity, String> {
    List<MarketEntity> findByStatus(core.market.MarketStatus status);

    /**
     * {@code SELECT ... FOR UPDATE} on the markets row. Used by the resolve path
     * to serialize against in-flight buys. See ADR-0004.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MarketEntity m where m.marketId = :marketId")
    Optional<MarketEntity> findByMarketIdForUpdate(@Param("marketId") String marketId);

    /**
     * {@code SELECT ... FOR SHARE} on the markets row. Used by the buy path so
     * multiple buys coexist but resolve's {@code FOR UPDATE} blocks them. See ADR-0004.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select m from MarketEntity m where m.marketId = :marketId")
    Optional<MarketEntity> findByMarketIdForShare(@Param("marketId") String marketId);

    /**
     * Flip the Market to {@code RESOLVED} only if every {@code position_settlements}
     * row for it is gone. Native because the {@code UPDATE ... WHERE NOT EXISTS}
     * pattern with a cross-table subquery is awkward in HQL. The {@code UPDATE}
     * implicitly takes a row lock that serializes concurrent flippers; the
     * {@code status = 'RESOLUTION_PENDING'} predicate makes the transition idempotent.
     *
     * <p>{@code resolved_at} is intentionally NOT updated here — it timestamps when
     * the outcome was decided (set at the {@code OPEN -> RESOLUTION_PENDING}
     * transition by {@link core.market.Market#resolveMarket}) and is preserved
     * through Settlement.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            UPDATE market.markets
               SET status = 'RESOLVED'
             WHERE market_id = :marketId
               AND status = 'RESOLUTION_PENDING'
               AND NOT EXISTS (
                 SELECT 1 FROM market.position_settlements ps
                  WHERE ps.market_id = :marketId
               )
            """, nativeQuery = true)
    int flipResolvedIfFullySettled(@Param("marketId") String marketId);

    /**
     * Flip the Market to {@code SETTLEMENT_FAILED}. Native for symmetry with the
     * resolved flip. Idempotent via the {@code status = 'RESOLUTION_PENDING'} guard.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            UPDATE market.markets
               SET status = 'SETTLEMENT_FAILED'
             WHERE market_id = :marketId
               AND status = 'RESOLUTION_PENDING'
            """, nativeQuery = true)
    int flipSettlementFailed(@Param("marketId") String marketId);

    /**
     * Admin retry: flip {@code SETTLEMENT_FAILED -> RESOLUTION_PENDING}.
     * Idempotent via the status predicate; a no-op if the Market is in any
     * other state (already RESOLVED, still OPEN, etc.).
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            UPDATE market.markets
               SET status = 'RESOLUTION_PENDING'
             WHERE market_id = :marketId
               AND status = 'SETTLEMENT_FAILED'
            """, nativeQuery = true)
    int flipBackToResolutionPending(@Param("marketId") String marketId);
}
