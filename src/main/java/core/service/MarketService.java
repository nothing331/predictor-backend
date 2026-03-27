package core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import api.dto.GetAllMarket;
import core.event.MarketCreatedEvent;
import core.event.MarketResolvedEvent;
import core.market.Market;
import core.market.MarketStats;
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
    private final ApplicationEventPublisher eventPublisher;
    private final TradeService tradeService;

    public MarketService(MarketRepository repository, MarketStore marketStore, SettlementEngine settlementEngine,
            UserService userService, ApplicationEventPublisher eventPublisher, TradeService tradeService) {
        this.repository = repository;
        this.marketStore = marketStore;
        this.settlementEngine = settlementEngine;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.tradeService = tradeService;
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
        try {
            repository.saveAll(List.of(market));
        } catch (Exception e) {
            throw e;
        }

        marketStore.put(market);

        eventPublisher.publishEvent(new MarketCreatedEvent(
                market.getMarketId(),
                market.getMarketName(),
                currentActorUserId()));

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
            getAllMarkets.add(toGetAllMarket(market));
        }
        return getAllMarkets;
    }

    public GetAllMarket getMarketById(String marketId) {
        Market market = marketStore.get(marketId);
        if (market == null) {
            return null;
        }
        return toGetAllMarket(market);
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

        // 4. Publish Event
        eventPublisher.publishEvent(new MarketResolvedEvent(
                market.getMarketId(),
                outcomeId,
                currentActorUserId()));
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

    private GetAllMarket toGetAllMarket(Market market) {
        List<MarketStats> outcomes = new ArrayList<>();

        boolean isResolved = market.getStatus() == core.market.MarketStatus.RESOLVED;
        Outcome winner = market.getResolvedOutcome();

        // After resolution, LMSR prices are stale — emit settled certainties instead.
        double yesProb = isResolved ? (winner == Outcome.YES ? 1.0 : 0.0) : market.getYesPrice();
        double noProb  = isResolved ? (winner == Outcome.NO  ? 1.0 : 0.0) : market.getNoPrice();

        outcomes.add(new MarketStats(Outcome.YES, market.getYesLabel(), yesProb));
        outcomes.add(new MarketStats(Outcome.NO, market.getNoLabel(), noProb));

        return new GetAllMarket(
                market.getMarketId(),
                market.getMarketName(),
                market.getStatus(),
                market.getResolvedOutcome(),
                market.getCategory(),
                outcomes,
                tradeService.getTotalCostByMarketId(market.getMarketId()));
    }

    private String currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
