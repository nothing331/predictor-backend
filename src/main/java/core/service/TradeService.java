package core.service;

import java.math.BigDecimal;
import java.util.Collection;

import org.springframework.stereotype.Service;

import core.trade.Trade;
import core.repository.TradeRepository;

@Service
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

        if (trade.getUserId() == null || trade.getUserId().trim().isEmpty()) {
            throw new IllegalStateException("Trade userId cannot be null or empty");
        }

        if (trade.getMarketId() == null || trade.getMarketId().trim().isEmpty()) {
            throw new IllegalStateException("Trade marketId cannot be null or empty");
        }

        if (trade.getShareCount() < 0) {
            throw new IllegalStateException("Trade shareCount cannot be less than");
        }

        if (trade.getCost() == null || trade.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Trade cost cannot be null or less than or equal to 0");
        }

        if (trade.getCreatedAt() == null) {
            throw new IllegalStateException("Trade timeStamp cannot be null");
        }
    }
}
