package core.store;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import core.market.Market;
import core.repository.MarketRepository;

@Component
public class MarketStore {
    private final Map<String, Market> markets = new ConcurrentHashMap<>();
    private final MarketRepository repository;

    public MarketStore(MarketRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        Collection<Market> loaded = repository.loadAll();
        if (loaded != null) {
            loaded.forEach(m -> {
                m.validate();
                markets.put(m.getMarketId(), m);
            });
        }
    }

    public Market get(String id) {
        return markets.get(id);
    }

    public void put(Market market) {
        markets.put(market.getMarketId(), market);
    }

    public Collection<Market> getAll() {
        return markets.values();
    }
}
