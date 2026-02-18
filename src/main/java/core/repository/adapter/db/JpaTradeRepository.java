package core.repository.adapter.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import db.entity.TradeEntity;

@Repository
public interface JpaTradeRepository extends JpaRepository<TradeEntity, Long> {
}
