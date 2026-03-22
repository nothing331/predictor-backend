package api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import core.market.Outcome;

public class MarketHistoryPoint {
    private Instant timestamp;
    private double yesProbability;
    private double noProbability;
    private String eventType;
    private String tradeId;
    private Outcome outcome;
    private Double sharesBought;
    private BigDecimal cost;

    public MarketHistoryPoint() {
    }

    public MarketHistoryPoint(Instant timestamp, double yesProbability, double noProbability, String eventType,
            String tradeId, Outcome outcome, Double sharesBought, BigDecimal cost) {
        this.timestamp = timestamp;
        this.yesProbability = yesProbability;
        this.noProbability = noProbability;
        this.eventType = eventType;
        this.tradeId = tradeId;
        this.outcome = outcome;
        this.sharesBought = sharesBought;
        this.cost = cost;
    }

    public static MarketHistoryPoint initial(Instant timestamp, double yesProbability, double noProbability) {
        return new MarketHistoryPoint(timestamp, yesProbability, noProbability, "INITIAL", null, null, null, null);
    }

    public static MarketHistoryPoint trade(Instant timestamp, double yesProbability, double noProbability,
            String tradeId, Outcome outcome, double sharesBought, BigDecimal cost) {
        return new MarketHistoryPoint(timestamp, yesProbability, noProbability, "TRADE", tradeId, outcome,
                sharesBought, cost);
    }

    public static MarketHistoryPoint resolution(Instant timestamp, double yesProbability, double noProbability) {
        return new MarketHistoryPoint(timestamp, yesProbability, noProbability, "RESOLUTION", null, null, null, null);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getYesProbability() {
        return yesProbability;
    }

    public double getNoProbability() {
        return noProbability;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTradeId() {
        return tradeId;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public Double getSharesBought() {
        return sharesBought;
    }

    public BigDecimal getCost() {
        return cost;
    }
}
