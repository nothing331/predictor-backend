package db.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite key for {@link PositionSettlementEntity}: (market_id, user_id).
 * Used as the {@code @IdClass} for the JPA mapping.
 */
public class PositionSettlementId implements Serializable {

    private String marketId;
    private String userId;

    public PositionSettlementId() {
    }

    public PositionSettlementId(String marketId, String userId) {
        this.marketId = marketId;
        this.userId = userId;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionSettlementId other)) return false;
        return Objects.equals(marketId, other.marketId) && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(marketId, userId);
    }
}
