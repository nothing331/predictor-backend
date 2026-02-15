package core.service;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Service;

import api.dto.GetAllMarket;
import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.repository.MarketRepository;

@Service
public class MarketService {
    private final MarketRepository repository;

    public MarketService(MarketRepository repository) {
        this.repository = repository;
    }

    public boolean addMarket(Market market) {
        validateMarket(market);
        Collection<Market> markets = loadAll();
        for (Market m : markets) {
            if (m.getMarketId().equals(market.getMarketId()) ||
                    m.getMarketName().equalsIgnoreCase(market.getMarketName())) {
                return false;
            }
        }
        markets.add(market);
        saveAll(markets);
        return true;
    }

    public Collection<GetAllMarket> getAll(String query) {
        Collection<Market> markets = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            markets = loadAll();
        } else {
            markets = repository.loadByStatus(query);

        }
        Collection<GetAllMarket> getAllMarkets = new ArrayList<>();
        for (Market market : markets) {
            getAllMarkets.add(new GetAllMarket(market.getMarketId(), market.getMarketName(),
                    market.getMarketDescription(), market.getStatus(), market.getResolvedOutcome()));
        }
        return getAllMarkets;
    }

    public GetAllMarket getMarketById(String marketId) {
        Market market = repository.loadById(marketId);
        if (market == null) {
            return null;
        }
        return new GetAllMarket(market.getMarketId(), market.getMarketName(),
                market.getMarketDescription(), market.getStatus(), market.getResolvedOutcome());
    }

    public void resolveMarket(String marketId, String outcomeId) {
        Outcome outcome;
        try {
            outcome = Outcome.valueOf(outcomeId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid outcome: " + outcomeId);
        }

        Collection<Market> markets = loadAll();
        Market market = markets.stream()
                .filter(m -> m.getMarketId().equals(marketId))
                .findFirst()
                .orElse(null);

        if (market == null) {
            throw new IllegalArgumentException("Market not found: " + marketId);
        }

        market.resolveMarket(outcome);
        repository.saveAll(markets);
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
