package core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import api.dto.GetAllMarket;
import core.cache.MarketReadCache;
import core.event.MarketCreatedEvent;
import core.event.MarketResolvedEvent;
import core.event.MarketSettlementCompletedEvent;
import core.market.Market;
import core.market.MarketStats;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.repository.port.PositionSettlementRepository;
import jakarta.transaction.Transactional;

/**
 * Market read/write service. After step 6 of the async-settlement refactor,
 * the in-memory {@code MarketStore} is gone and every read goes to Postgres.
 * Read endpoints get fronted by the Redis cache in a later step; the
 * correctness paths ({@link #resolveMarket}, used by {@code TradeService.buy})
 * always read with a DB row lock.
 *
 * <p>See:
 * <ul>
 *   <li>{@code docs/adr/0001-remove-in-memory-stores-from-correctness-paths.md}</li>
 *   <li>{@code docs/adr/0002-async-settlement-via-postgres-queue.md}</li>
 *   <li>{@code docs/adr/0004-lock-order-market-then-user.md}</li>
 * </ul>
 */
@Service
public class MarketService {
    private final MarketRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final TradeService tradeService;
    private final PositionSettlementRepository positionSettlementRepository;

    /** {@code null} when {@code spring.data.redis.url} is absent (unit-test profile). */
    private final MarketReadCache cache;

    public MarketService(MarketRepository repository,
            ApplicationEventPublisher eventPublisher, TradeService tradeService,
            PositionSettlementRepository positionSettlementRepository,
            ObjectProvider<MarketReadCache> cacheProvider) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.tradeService = tradeService;
        this.positionSettlementRepository = positionSettlementRepository;
        this.cache = cacheProvider.getIfAvailable();
    }

    public boolean addMarket(Market market) {
        market.validate();
        // App-level uniqueness on (marketId, marketName). No DB unique index on
        // marketName today; the scan is cheap because the markets table is small.
        Collection<Market> markets = repository.loadAll();
        for (Market m : markets) {
            if (m.getMarketId().equals(market.getMarketId()) ||
                    m.getMarketName().equalsIgnoreCase(market.getMarketName())) {
                return false;
            }
        }
        repository.saveAll(List.of(market));

        eventPublisher.publishEvent(new MarketCreatedEvent(
                market.getMarketId(),
                market.getMarketName(),
                currentActorUserId()));

        return true;
    }

    public Collection<GetAllMarket> getAll(String query) {
        // Only cache the three well-known list keys; an unknown query string
        // bypasses cache entirely (and returns empty today — see below).
        boolean cacheable = (query == null || query.isEmpty())
                || "OPEN".equalsIgnoreCase(query) || "RESOLVED".equalsIgnoreCase(query);

        if (cacheable && cache != null) {
            List<GetAllMarket> hit = cache.getList(query);
            if (hit != null) return hit;
        }

        Collection<Market> markets;
        if (query == null || query.isEmpty()) {
            markets = repository.loadAll();
        } else if ("OPEN".equalsIgnoreCase(query) || "RESOLVED".equalsIgnoreCase(query)) {
            markets = repository.loadByStatus(query.toUpperCase());
        } else {
            return List.of();
        }
        List<GetAllMarket> dtos = new ArrayList<>();
        for (Market market : markets) {
            dtos.add(toGetAllMarket(market));
        }

        if (cacheable && cache != null) {
            cache.putList(query, dtos);
        }
        return dtos;
    }

    public GetAllMarket getMarketById(String marketId) {
        if (cache != null) {
            GetAllMarket hit = cache.getDetail(marketId);
            if (hit != null) return hit;
        }
        Market market = repository.loadById(marketId);
        if (market == null) {
            return null;
        }
        GetAllMarket dto = toGetAllMarket(market);
        if (cache != null) {
            cache.putDetail(marketId, dto);
        }
        return dto;
    }

    /**
     * Record the winning {@link Outcome} and enqueue per-Position settlement
     * work. Runs in ONE transaction so the {@code OPEN -> RESOLUTION_PENDING}
     * status flip and the {@code position_settlements} inserts commit together;
     * concurrent buys (holding {@code FOR SHARE} on the Market row) either
     * commit before this transaction or get blocked until it commits and then
     * fail the {@code status == OPEN} check.
     *
     * <p>Does NOT pay anyone out — that's the Settlement Worker's job.
     */
    @Transactional
    public void resolveMarket(String marketId, String outcomeId) {
        Outcome outcome;
        try {
            outcome = Outcome.valueOf(outcomeId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid outcome: " + outcomeId);
        }

        // Market FOR UPDATE — see ADR-0004.
        Market market = repository.loadByIdForUpdate(marketId);
        if (market == null) {
            throw new IllegalArgumentException("Market not found: " + marketId);
        }

        // Transition OPEN -> RESOLUTION_PENDING and record the outcome.
        market.resolveMarket(outcome);
        market.validate();
        repository.saveAll(List.of(market));

        // Enqueue one row per unsettled Position in this Market. Single
        // INSERT ... SELECT ... ON CONFLICT DO NOTHING; idempotent.
        int enqueued = positionSettlementRepository.enqueueUnsettledPositionsForMarket(marketId);

        // No unsettled Positions => no settlement work => the Settlement Worker
        // would never run for this Market, leaving it stuck in RESOLUTION_PENDING
        // forever. Flip straight to RESOLVED here. markResolvedIfFullySettled is
        // guarded by NOT EXISTS, so it's a safe no-op if rows were actually
        // enqueued (or pre-existed). See ADR-0003 / "empty market" edge case.
        if (enqueued == 0 && repository.markResolvedIfFullySettled(marketId)) {
            eventPublisher.publishEvent(new MarketSettlementCompletedEvent(marketId));
        }

        eventPublisher.publishEvent(new MarketResolvedEvent(
                market.getMarketId(),
                outcomeId,
                currentActorUserId()));
    }

    public void saveAll(Collection<Market> markets) {
        if (markets == null || markets.isEmpty()) {
            return;
        }
        for (Market market : markets) {
            market.validate();
        }
        repository.saveAll(markets);
    }

    public Collection<Market> loadAll() {
        return repository.loadAll();
    }

    private GetAllMarket toGetAllMarket(Market market) {
        List<MarketStats> outcomes = new ArrayList<>();

        // Branch on resolvedOutcome != null rather than status == RESOLVED so that
        // RESOLUTION_PENDING and SETTLEMENT_FAILED markets also show settled
        // certainties (1.0 / 0.0) instead of stale LMSR prices.
        Outcome winner = market.getResolvedOutcome();
        boolean hasOutcome = winner != null;

        double yesProb = hasOutcome ? (winner == Outcome.YES ? 1.0 : 0.0) : market.getYesPrice();
        double noProb  = hasOutcome ? (winner == Outcome.NO  ? 1.0 : 0.0) : market.getNoPrice();

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
