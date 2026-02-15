package core.service;

import java.math.BigDecimal;
import java.util.Collection;

import org.springframework.stereotype.Service;

import api.dto.BuyRequest;
import core.trade.Trade;
import core.trade.TradeEngine;
import core.user.User;
import core.lmsr.PricingEngine;
import core.market.Market;
import core.market.Outcome;
import core.repository.MarketRepository;
import core.repository.TradeRepository;
import core.service.UserService;

import core.store.MarketStore;

@Service
public class TradeService {
    private final TradeRepository repository;
    private final UserService userService;
    private final MarketRepository marketRepository;
    private final TradeEngine tradeEngine;
    private final MarketStore marketStore;

    public TradeService(TradeRepository repository, UserService userService, MarketRepository marketRepository,
            TradeEngine tradeEngine, MarketStore marketStore) {
        this.repository = repository;
        this.userService = userService;
        this.marketRepository = marketRepository;
        this.tradeEngine = tradeEngine;
        this.marketStore = marketStore;
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

    public void buy(BuyRequest request, String userId, String marketId) {
        User user = userService.getUserById(userId);
        Market market = marketStore.get(marketId);

        if (market == null) {
            throw new IllegalArgumentException("Market not found: " + marketId);
        }
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        Outcome outcome;
        try {
            outcome = Outcome.valueOf(request.getOutcome().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid outcome: " + request.getOutcome());
        }

        // Calculate how many shares can be bought for the given amount (budget)
        double sharesToBuy = PricingEngine.sharesForAmount(
                request.getAmount(),
                market.getQYes(),
                market.getQNo(),
                market.getLiquidity(),
                outcome == Outcome.YES);

        if (sharesToBuy <= 0) {
            throw new IllegalArgumentException("Amount " + request.getAmount() + " is too low to buy any shares.");
        }

        // Execute valid trade
        Trade trade = tradeEngine.executeTrade(user, market, outcome, sharesToBuy);

        // Persist all changes
        userService.saveUser(user);
        marketRepository.saveAll(java.util.Collections.singletonList(market));
        repository.saveAll(java.util.Collections.singletonList(trade));
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
