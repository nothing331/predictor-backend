package api.dto;

import core.market.MarketStatus;
import core.market.Outcome;

public class GetAllMarket {

    private String marketId;
    private String marketName;
    private String marketDescription;
    private MarketStatus status;
    private Outcome resolvedOutcome;

    public GetAllMarket() {
    }

    public GetAllMarket(String marketId, String marketName, String marketDescription, MarketStatus status,
            Outcome resolvedOutcome) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.marketDescription = marketDescription;
        this.status = status;
        this.resolvedOutcome = resolvedOutcome;
    }

    public String getMarketId() {
        return marketId;
    }

    public String getMarketName() {
        return marketName;
    }

    public String getMarketDescription() {
        return marketDescription;
    }

    public MarketStatus getStatus() {
        return status;
    }

    public Outcome getResolvedOutcome() {
        return resolvedOutcome;
    }
}
