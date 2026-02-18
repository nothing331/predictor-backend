package db.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "positions", schema = "market", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "market_id" })
})
public class PositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "position_id")
    private Long positionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "market_id", nullable = false)
    private String marketId;

    @Column(name = "yes_shares")
    private BigDecimal yesShares;

    @Column(name = "no_shares")
    private BigDecimal noShares;

    protected PositionEntity() {
    }

    public PositionEntity(Long positionId, String userId, String marketId, BigDecimal yesShares, BigDecimal noShares) {
        this.positionId = positionId;
        this.userId = userId;
        this.marketId = marketId;
        this.yesShares = yesShares;
        this.noShares = noShares;
    }

    // getters and setters
    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public BigDecimal getYesShares() {
        return yesShares;
    }

    public void setYesShares(BigDecimal yesShares) {
        this.yesShares = yesShares;
    }

    public BigDecimal getNoShares() {
        return noShares;
    }

    public void setNoShares(BigDecimal noShares) {
        this.noShares = noShares;
    }
}
