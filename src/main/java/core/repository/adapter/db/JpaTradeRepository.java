package core.repository.adapter.db;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import db.entity.TradeEntity;

@Repository
public interface JpaTradeRepository extends JpaRepository<TradeEntity, Long> {
    List<TradeEntity> findByMarketId(String marketId);

    List<TradeEntity> findByMarketIdOrderByTradedAtAsc(String marketId);

    List<TradeEntity> findByUserIdOrderByTradedAtDesc(String userId);

    List<TradeEntity> findByUserIdAndMarketIdOrderByTradedAtDesc(String userId, String marketId);

    @Query("SELECT COALESCE(SUM(t.cost), 0) FROM TradeEntity t WHERE t.marketId = :marketId")
    BigDecimal sumCostByMarketId(@Param("marketId") String marketId);
}
