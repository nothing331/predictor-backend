package api.dto;

import java.math.BigDecimal;
import java.util.List;

import core.market.MarketStats;
import core.market.MarketStatus;
import core.market.Outcome;

public class GetAllMarket {

    private String marketId;
    private String marketName;
    private Outcome resolvedOutcome;
    private MarketStatus status;
    private String category;
    private List<MarketStats> outcomes;
    private BigDecimal totalValue;


    public GetAllMarket() {
    }

    public GetAllMarket(String marketId, String marketName, MarketStatus status,
            Outcome resolvedOutcome, String category, List<MarketStats> outcomes, BigDecimal totalValue) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.status = status;
        this.resolvedOutcome = resolvedOutcome;
        this.category = category;
        this.outcomes = outcomes;
        this.totalValue = totalValue;
    }

    public String getMarketId() {
        return marketId;
    }

    public String getMarketName() {
        return marketName;
    }


    public MarketStatus getStatus() {
        return status;
    }

    public Outcome getResolvedOutcome() {
        return resolvedOutcome;
    }

    public String getCategory() {
        return category;
    }

    public List<MarketStats> getOutcomes() {
        return outcomes;
    }

    public BigDecimal getTotalValue() {
        return totalValue;
    }
}
