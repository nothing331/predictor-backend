package db.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import core.market.MarketStatus;
import core.market.Outcome;

@Entity
@Table(name = "markets", schema = "market")
public class MarketEntity {
    @Id
    private String marketId;

    @Column(nullable = false, name = "market_name", length = 100, unique = true)
    private String marketName;

    @Column(name = "market_description")
    private String marketDescription;

    @Column(nullable = false, name = "q_yes")
    private BigDecimal qYes;

    @Column(nullable = false, name = "q_no")
    private BigDecimal qNo;

    @Column(name = "liquidity_param")
    private BigDecimal liquidityParam;

    @Column(nullable = false, name = "status")
    @Enumerated(EnumType.STRING)
    private MarketStatus status;

    @Column(name = "resolved_outcome")
    @Enumerated(EnumType.STRING)
    private Outcome resolvedOutcome;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "resolved_at")
    private Timestamp resolvedAt;

    protected MarketEntity() {
    }

    public MarketEntity(String marketId, String marketName, String marketDescription, BigDecimal qYes, BigDecimal qNo,
            BigDecimal liquidityParam, MarketStatus status, Outcome resolvedOutcome, Timestamp createdAt,
            Timestamp resolvedAt) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.marketDescription = marketDescription;
        this.qYes = qYes;
        this.qNo = qNo;
        this.liquidityParam = liquidityParam;
        this.status = status;
        this.resolvedOutcome = resolvedOutcome;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    // getters and setters
    public String getMarketId() {
        return marketId;
    }

    public void setMarketId(String marketId) {
        this.marketId = marketId;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getMarketDescription() {
        return marketDescription;
    }

    public void setMarketDescription(String marketDescription) {
        this.marketDescription = marketDescription;
    }

    public BigDecimal getQYes() {
        return qYes;
    }

    public void setQYes(BigDecimal qYes) {
        this.qYes = qYes;
    }

    public BigDecimal getQNo() {
        return qNo;
    }

    public void setQNo(BigDecimal qNo) {
        this.qNo = qNo;
    }

    public BigDecimal getLiquidityParam() {
        return liquidityParam;
    }

    public void setLiquidityParam(BigDecimal liquidityParam) {
        this.liquidityParam = liquidityParam;
    }

    public MarketStatus getStatus() {
        return status;
    }

    public void setStatus(MarketStatus status) {
        this.status = status;
    }

    public Outcome getResolvedOutcome() {
        return resolvedOutcome;
    }

    public void setResolvedOutcome(Outcome resolvedOutcome) {
        this.resolvedOutcome = resolvedOutcome;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
