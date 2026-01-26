package core.service;

import java.util.Collection;
import core.trade.Trade;
import core.repository.TradeRepository;

public class TradeService {
    private final TradeRepository repository;

    public TradeService(TradeRepository repository) {
        this.repository = repository;
    }

    public void saveAll(Collection<Trade> trades) {
        if (trades != null) {
            for (Trade trade : trades) {
                validateTrade(trade);
            }
        }
        repository.saveAll(trades);
    }

    public Collection<Trade> loadAll() {
        Collection<Trade> trades = repository.loadAll();
        if (trades != null) {
            for (Trade trade : trades) {
                validateTrade(trade);
            }
        }
        return trades;
    }

    private void validateTrade(Trade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null");
        }

        // Validate userId is present
        if (trade.getUserId() == null || trade.getUserId().trim().isEmpty()) {
            throw new IllegalStateException("Trade userId cannot be null or empty");
        }

        if (trade.getMarketId() == null || trade.getMarketId().trim().isEmpty()) {
            throw new IllegalStateException("Trade marketId cannot be null or empty");
        }
    }
}
