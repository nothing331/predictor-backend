package core.service;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Service;

import api.dto.GetAllMarket;
import core.market.Market;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.settlement.SettlementEngine;
import core.store.MarketStore;
import core.user.User;
import jakarta.transaction.Transactional;

@Service
public class MarketService {
    private final MarketRepository repository;
    private final MarketStore marketStore;
    private final SettlementEngine settlementEngine;
    private final UserService userService;

    public MarketService(MarketRepository repository, MarketStore marketStore, SettlementEngine settlementEngine,
            UserService userService) {
        this.repository = repository;
        this.marketStore = marketStore;
        this.settlementEngine = settlementEngine;
        this.userService = userService;
    }

    public boolean addMarket(Market market) {
        market.validate();
        Collection<Market> markets = marketStore.getAll();
        for (Market m : markets) {
            if (m.getMarketId().equals(market.getMarketId()) ||
                    m.getMarketName().equalsIgnoreCase(market.getMarketName())) {
                return false;
            }
        }
        marketStore.put(market);
        saveAll(marketStore.getAll());
        return true;
    }

    public Collection<GetAllMarket> getAll(String query) {
        Collection<Market> markets;
        if (query == null || query.isEmpty()) {
            markets = marketStore.getAll();
        } else {
            if ("OPEN".equalsIgnoreCase(query) || "RESOLVED".equalsIgnoreCase(query)) {
                markets = marketStore.getAll().stream()
                        .filter(m -> m.getStatus().toString().equalsIgnoreCase(query))
                        .toList();
            } else {
                markets = new ArrayList<>();
            }
        }
        Collection<GetAllMarket> getAllMarkets = new ArrayList<>();
        for (Market market : markets) {
            getAllMarkets.add(new GetAllMarket(market.getMarketId(), market.getMarketName(),
                    market.getMarketDescription(), market.getStatus(), market.getResolvedOutcome()));
        }
        return getAllMarkets;
    }

    public GetAllMarket getMarketById(String marketId) {
        Market market = marketStore.get(marketId);
        if (market == null) {
            return null;
        }
        return new GetAllMarket(market.getMarketId(), market.getMarketName(),
                market.getMarketDescription(), market.getStatus(), market.getResolvedOutcome());
    }

    @Transactional
    public void resolveMarket(String marketId, String outcomeId) {
        Outcome outcome;
        try {
            outcome = Outcome.valueOf(outcomeId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid outcome: " + outcomeId);
        }

        Market market = marketStore.get(marketId);

        if (market == null) {
            throw new IllegalArgumentException("Market not found: " + marketId);
        }

        // 1. Resolve market first (required by SettlementEngine)
        market.resolveMarket(outcome);

        // 2. Load users and settle
        Collection<User> users = userService.loadAll();
        settlementEngine.settleMarket(market, users);

        // 3. Persist state
        // Persist users (balances updated)
        userService.saveAll(users);
        // Persist markets (status updated)
        saveAll(marketStore.getAll());
    }

    public void saveAll(Collection<Market> markets) {
        if (markets != null) {
            for (Market market : markets) {
                market.validate();
                marketStore.put(market);
            }
        }
        repository.saveAll(marketStore.getAll());
    }

    public Collection<Market> loadAll() {
        return marketStore.getAll();
    }
}
