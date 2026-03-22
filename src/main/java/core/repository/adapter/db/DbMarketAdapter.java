package core.repository.adapter.db;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import core.market.Market;
import core.market.MarketStatus;
import core.repository.port.MarketRepository;
import db.entity.MarketEntity;

@Repository("marketDbAdapter")
public class DbMarketAdapter implements MarketRepository {

    private final JpaMarketRepository jpaMarketRepository;

    public DbMarketAdapter(JpaMarketRepository jpaMarketRepository) {
        this.jpaMarketRepository = jpaMarketRepository;
    }

    @Override
    public void saveAll(Collection<Market> markets) {
        List<MarketEntity> entities = markets.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        jpaMarketRepository.saveAll(entities);
    }

    @Override
    public Collection<Market> loadAll() {
        return jpaMarketRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Market loadById(String marketId) {
        return jpaMarketRepository.findById(marketId)
                .map(this::toDomain)
                .orElse(null);
    }

    @Override
    public Collection<Market> loadByStatus(String status) {
        try {
            MarketStatus marketStatus = MarketStatus.valueOf(status.toUpperCase());
            return jpaMarketRepository.findByStatus(marketStatus).stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
    }

    private MarketEntity toEntity(Market market) {
        return new MarketEntity(
                market.getMarketId(),
                market.getMarketName(),
                market.getMarketDescription(),
                BigDecimal.valueOf(market.getQYes()),
                BigDecimal.valueOf(market.getQNo()),
                BigDecimal.valueOf(market.getLiquidity()),
                market.getStatus(),
                market.getResolvedOutcome(),
                market.getCategory(),
                market.getYesLabel(),
                market.getNoLabel(),
                market.getCreatedAt() != null ? Timestamp.from(market.getCreatedAt()) : null,
                market.getResolvedAt() != null ? Timestamp.from(market.getResolvedAt()) : null
        );
    }

    private Market toDomain(MarketEntity entity) {
        // Market constructor: (String marketId, String marketName, String
        // marketDescription, double liquidity)
        Market market = new Market(
                entity.getMarketId(),
                entity.getMarketName(),
                entity.getMarketDescription(),
                entity.getLiquidityParam().doubleValue());

        // Use reflection or setters if available in Domain object to set state
        // correctly
        // Market.java: setQYes/setQNo are deprecated/internal but available via json
        // setters or similar?
        // Actually Market has setQYes/setQNo.
        // And resolveMarket sets status/outcome.

        // Since we are restoring from persistence, we need to bypass business logic
        // checks if possible
        // but Market.setQYes checks if market is OPEN. If market is RESOLVED, we can't
        // set QYes?
        // This is a domain model issue.
        // However, let's try to set them.

        try {
            // Reflection is safer to bypass "is open" checks
            java.lang.reflect.Field qYesField = Market.class.getDeclaredField("qYes");
            qYesField.setAccessible(true);
            qYesField.setDouble(market, entity.getQYes().doubleValue());

            java.lang.reflect.Field qNoField = Market.class.getDeclaredField("qNo");
            qNoField.setAccessible(true);
            qNoField.setDouble(market, entity.getQNo().doubleValue());

            java.lang.reflect.Field statusField = Market.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(market, entity.getStatus());

            java.lang.reflect.Field resolvedOutcomeField = Market.class.getDeclaredField("resolvedOutcome");
            resolvedOutcomeField.setAccessible(true);
            resolvedOutcomeField.set(market, entity.getResolvedOutcome());

            if (entity.getCategory() != null) {
                java.lang.reflect.Field categoryField = Market.class.getDeclaredField("category");
                categoryField.setAccessible(true);
                categoryField.set(market, entity.getCategory());
            }

            if (entity.getYesLabel() != null) {
                java.lang.reflect.Field yesLabelField = Market.class.getDeclaredField("yesLabel");
                yesLabelField.setAccessible(true);
                yesLabelField.set(market, entity.getYesLabel());
            }

            if (entity.getNoLabel() != null) {
                java.lang.reflect.Field noLabelField = Market.class.getDeclaredField("noLabel");
                noLabelField.setAccessible(true);
                noLabelField.set(market, entity.getNoLabel());
            }

            if (entity.getCreatedAt() != null) {
                java.lang.reflect.Field createdAtField = Market.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(market, entity.getCreatedAt().toInstant());
            }

            if (entity.getResolvedAt() != null) {
                java.lang.reflect.Field resolvedAtField = Market.class.getDeclaredField("resolvedAt");
                resolvedAtField.setAccessible(true);
                resolvedAtField.set(market, entity.getResolvedAt().toInstant());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to map MarketEntity to Market", e);
        }

        return market;
    }
}
