package core.service;

import java.util.Collection;

import core.market.Market;
import core.market.MarketStatus;
import core.repository.MarketRepository;

public class MarketService {
    private final MarketRepository repository;

    public MarketService(MarketRepository repository) {
        this.repository = repository;
    }

    public void saveAll(Collection<Market> markets) {
        if (markets != null) {
            for (Market market : markets) {
                validateMarket(market);
            }
        }
        repository.saveAll(markets);
    }

    public Collection<Market> loadAll() {
        Collection<Market> markets = repository.loadAll();
        if (markets != null) {
            for (Market market : markets) {
                validateMarket(market);
            }
        }
        return markets;
    }

    private void validateMarket(Market market) {
        if (market == null) {
            throw new IllegalArgumentException("Market cannot be null");
        }
        if (market.getStatus() == null) {
            throw new IllegalStateException("Market status cannot be null for market: " + market.getMarketId());
        }
        // Liquidity check (b > 0 usually, but let's just check it's not NaN or
        // infinite)
        if (Double.isNaN(market.getLiquidity()) || Double.isInfinite(market.getLiquidity())
                || market.getLiquidity() <= 0) {
            throw new IllegalStateException("Invalid liquidity for market: " + market.getMarketId());
        }

        // Outcome resolution check
        if (market.getStatus() == MarketStatus.RESOLVED && market.getResolvedOutcome() == null) {
            throw new IllegalStateException("Resolved market must have a resolved outcome: " + market.getMarketId());
        }
        if (market.getStatus() == MarketStatus.OPEN && market.getResolvedOutcome() != null) {
            throw new IllegalStateException("Open market cannot have a resolved outcome: " + market.getMarketId());
        }
    }
}
