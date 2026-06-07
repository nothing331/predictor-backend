package core.repository.adapter.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import db.entity.PositionEntity;
import java.util.List;

@Repository
public interface JpaPositionRepository extends JpaRepository<PositionEntity, Long> {
    List<PositionEntity> findByUserId(String userId);

    void deleteByUserId(String userId); // Caution: deletes all positions for reload? Or standard update?

    /**
     * Return the userIds of every unsettled Position in this Market, ordered for
     * stability. Used by {@code MarketService.resolveMarket} to populate the
     * {@code position_settlements} work queue inside the Resolution transaction.
     */
    @Query("""
            select p.userId
              from PositionEntity p
             where p.marketId = :marketId
               and p.settled = false
             order by p.userId
            """)
    List<String> findUnsettledUserIdsByMarketId(@Param("marketId") String marketId);
}
