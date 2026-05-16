package core.repository.adapter.db;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import core.trade.Trade;
import core.repository.port.TradeRepository;
import db.entity.TradeEntity;

@Repository("tradeDbAdapter")
public class DbTradeAdapter implements TradeRepository {

    private final JpaTradeRepository jpaTradeRepository;

    public DbTradeAdapter(JpaTradeRepository jpaTradeRepository) {
        this.jpaTradeRepository = jpaTradeRepository;
    }

    @Override
    public Trade save(Trade trade) {
        return toDomain(jpaTradeRepository.save(toEntity(trade)));
    }

    @Override
    public void saveAll(Collection<Trade> trades) {
        List<TradeEntity> entities = trades.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaTradeRepository.saveAll(entities);
    }

    @Override
    public Collection<Trade> loadAll() {
        return jpaTradeRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Trade> loadByMarketId(String marketId) {
        return jpaTradeRepository.findByMarketId(marketId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Trade> loadByMarketIdOrdered(String marketId) {
        return jpaTradeRepository.findByMarketIdOrderByTradedAtAsc(marketId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Trade> loadByUserIdOrderedDesc(String userId) {
        return jpaTradeRepository.findByUserIdOrderByTradedAtDesc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Trade> loadByUserIdAndMarketIdOrderedDesc(String userId, String marketId) {
        return jpaTradeRepository.findByUserIdAndMarketIdOrderByTradedAtDesc(userId, marketId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Trade> loadByUserIdAndClientRequestId(String userId, String clientRequestId) {
        return jpaTradeRepository.findByUserIdAndClientRequestId(userId, clientRequestId)
                .map(this::toDomain);
    }

    @Override
    public BigDecimal sumCostByMarketId(String marketId) {
        return jpaTradeRepository.sumCostByMarketId(marketId);
    }

    private TradeEntity toEntity(Trade trade) {
        // TradeEntity uses auto-generated Long ID. We ignore trade.getTradeId() (String
        // UUID).
        // Unless we store it. But we don't have a column for it.
        // If we save a new Trade, ID is null.
        // If we update, we need ID. But Trade domain doesn't hold the Long ID.
        // So we can only support INSERTs effectively, or updates if we query by UUID
        // (which we can't).
        // Assuming Trade is immutable and only inserted.

        return new TradeEntity(
                parseIdAsLong(trade.getTradeId()),
                trade.getUserId(),
                trade.getMarketId(),
                trade.getOutcome(),
                java.math.BigDecimal.valueOf(trade.getShareCount()),
                trade.getCost(),
                Timestamp.from(trade.getCreatedAt()),
                trade.getClientRequestId());
    }

    private Trade toDomain(TradeEntity entity) {
        // Trade constructor: (String tradeId, String userId, String marketId, Outcome
        // outcome, double sharesBought, BigDecimal cost, Instant createdAt)
        // We use String.valueOf(entity.getTradeId()) as the ID.

        return new Trade(
                String.valueOf(entity.getTradeId()),
                entity.getUserId(),
                entity.getMarketId(),
                entity.getOutcome(),
                entity.getSharesBought().doubleValue(),
                entity.getCost(),
                entity.getTradedAt().toInstant(),
                entity.getClientRequestId());
    }

    private Long parseIdAsLong(String id) {
        if (id == null) {
            return null;
        }
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
