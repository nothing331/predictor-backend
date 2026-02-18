package core.repository.adapter.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import db.entity.MarketEntity;
import java.util.List;

@Repository
public interface JpaMarketRepository extends JpaRepository<MarketEntity, String> {
    List<MarketEntity> findByStatus(core.market.MarketStatus status);
}
