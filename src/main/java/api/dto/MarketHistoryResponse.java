package api.dto;

import java.util.List;

import core.market.MarketStatus;

public class MarketHistoryResponse {
    private String marketId;
    private MarketStatus status;
    private List<MarketHistoryPoint> points;

    public MarketHistoryResponse() {
    }

    public MarketHistoryResponse(String marketId, MarketStatus status, List<MarketHistoryPoint> points) {
        this.marketId = marketId;
        this.status = status;
        this.points = points;
    }

    public String getMarketId() {
        return marketId;
    }

    public MarketStatus getStatus() {
        return status;
    }

    public List<MarketHistoryPoint> getPoints() {
        return points;
    }
}
