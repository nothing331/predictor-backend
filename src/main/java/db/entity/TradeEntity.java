package db.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import core.market.Outcome;

@Entity
@Table(name = "trades", schema = "market")
public class TradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long tradeId;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false, name = "market_id")
    private String marketId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    @Column(nullable = false, name = "shares_bought")
    private BigDecimal sharesBought;

    @Column(nullable = false)
    private BigDecimal cost;

    @Column(name = "traded_at")
    private Timestamp tradedAt;

    protected TradeEntity() {
    }

    public TradeEntity(Long tradeId, String userId, String marketId, Outcome outcome, BigDecimal sharesBought,
            BigDecimal cost, Timestamp tradedAt) {
        this.tradeId = tradeId;
        this.userId = userId;
        this.marketId = marketId;
        this.outcome = outcome;
        this.sharesBought = sharesBought;
        this.cost = cost;
        this.tradedAt = tradedAt;
    }

    // getters and setters
    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
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

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public BigDecimal getSharesBought() {
        return sharesBought;
    }

    public void setSharesBought(BigDecimal sharesBought) {
        this.sharesBought = sharesBought;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Timestamp getTradedAt() {
        return tradedAt;
    }

    public void setTradedAt(Timestamp tradedAt) {
        this.tradedAt = tradedAt;
    }
}
