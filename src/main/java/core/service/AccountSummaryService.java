package core.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import api.dto.UserAccountSummaryResponse;
import api.dto.UserRecentMarketSummary;
import core.market.Market;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.trade.Trade;
import core.user.User;

@Service
public class AccountSummaryService {

    private static final int RECENT_MARKET_LIMIT = 3;

    private final UserService userService;
    private final TradeService tradeService;
    private final MarketRepository marketRepository;

    public AccountSummaryService(UserService userService, TradeService tradeService,
            MarketRepository marketRepository) {
        this.userService = userService;
        this.tradeService = tradeService;
        this.marketRepository = marketRepository;
    }

    public UserAccountSummaryResponse getSummary(String userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        List<Trade> orderedTrades = new ArrayList<>(tradeService.getTradesByUserIdOrderedDesc(userId));
        LinkedHashMap<String, MarketAggregate> selectedMarkets = initRecentMarkets(orderedTrades);

        if (selectedMarkets.isEmpty()) {
            return new UserAccountSummaryResponse(user.getUserId(), user.getBalance(), List.of());
        }

        accumulateTrades(orderedTrades, selectedMarkets);

        List<UserRecentMarketSummary> recentMarkets = new ArrayList<>();
        for (Map.Entry<String, MarketAggregate> entry : selectedMarkets.entrySet()) {
            // One DB read per recent market (capped at 3). The Redis cache
            // added in step 7 fronts repository.loadById so the hot path stays
            // cheap. See docs/adr/0005-redis-read-cache-cache-aside-delete-after-commit.md.
            Market market = marketRepository.loadById(entry.getKey());
            if (market == null) {
                continue;
            }

            MarketAggregate aggregate = entry.getValue();
            recentMarkets.add(new UserRecentMarketSummary(
                    market.getMarketId(),
                    market.getMarketName(),
                    market.getStatus(),
                    aggregate.lastTradedAt(),
                    market.getResolvedOutcome(),
                    aggregate.userYesShares(),
                    aggregate.userNoShares(),
                    currentYesChance(market),
                    currentNoChance(market),
                    BigDecimal.valueOf(aggregate.userYesShares()),
                    BigDecimal.valueOf(aggregate.userNoShares())));
        }

        return new UserAccountSummaryResponse(user.getUserId(), user.getBalance(), recentMarkets);
    }

    private LinkedHashMap<String, MarketAggregate> initRecentMarkets(Collection<Trade> orderedTrades) {
        Set<String> recentMarketIds = new LinkedHashSet<>();
        for (Trade trade : orderedTrades) {
            recentMarketIds.add(trade.getMarketId());
            if (recentMarketIds.size() == RECENT_MARKET_LIMIT) {
                break;
            }
        }

        LinkedHashMap<String, MarketAggregate> selectedMarkets = new LinkedHashMap<>();
        for (String marketId : recentMarketIds) {
            selectedMarkets.put(marketId, new MarketAggregate());
        }
        return selectedMarkets;
    }

    private void accumulateTrades(Collection<Trade> orderedTrades, LinkedHashMap<String, MarketAggregate> selectedMarkets) {
        for (Trade trade : orderedTrades) {
            MarketAggregate aggregate = selectedMarkets.get(trade.getMarketId());
            if (aggregate == null) {
                continue;
            }

            aggregate.record(trade);
        }
    }

    private double currentYesChance(Market market) {
        if (market.getResolvedOutcome() != null) {
            return market.getResolvedOutcome() == Outcome.YES ? 1.0 : 0.0;
        }
        return market.getYesPrice();
    }

    private double currentNoChance(Market market) {
        if (market.getResolvedOutcome() != null) {
            return market.getResolvedOutcome() == Outcome.NO ? 1.0 : 0.0;
        }
        return market.getNoPrice();
    }

    private static final class MarketAggregate {
        private double userYesShares;
        private double userNoShares;
        private Instant lastTradedAt;

        void record(Trade trade) {
            if (trade.getOutcome() == Outcome.YES) {
                userYesShares += trade.getShareCount();
            } else {
                userNoShares += trade.getShareCount();
            }

            if (lastTradedAt == null || trade.getCreatedAt().isAfter(lastTradedAt)) {
                lastTradedAt = trade.getCreatedAt();
            }
        }

        double userYesShares() {
            return userYesShares;
        }

        double userNoShares() {
            return userNoShares;
        }

        Instant lastTradedAt() {
            return lastTradedAt;
        }
    }
}
