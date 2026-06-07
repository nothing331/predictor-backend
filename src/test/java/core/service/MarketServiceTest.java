package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import core.cache.MarketReadCache;
import core.event.MarketResolvedEvent;
import core.event.MarketSettlementCompletedEvent;
import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.repository.port.PositionSettlementRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for {@link MarketService} under the async-settlement lifecycle:
 * resolve records the outcome (OPEN -> RESOLUTION_PENDING) and enqueues
 * settlement work; payouts happen later in the worker. See ADR-0002 / ADR-0003.
 */
public class MarketServiceTest {

    @Mock
    private MarketRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private TradeService tradeService;
    @Mock
    private PositionSettlementRepository positionSettlementRepository;
    @Mock
    private ObjectProvider<MarketReadCache> cacheProvider;

    private MarketService marketService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // cacheProvider.getIfAvailable() returns null by default (Mockito) =>
        // MarketService.cache == null => no Redis path exercised in unit tests.
        marketService = new MarketService(repository, eventPublisher, tradeService,
                positionSettlementRepository, cacheProvider);
    }

    @Test
    public void resolveMarket_withUnsettledPositions_recordsOutcomeAndEnqueues() {
        String marketId = "market-1";
        Market market = new Market(marketId, "Test Market", "Desc");

        // Resolve reads the Market via repository.loadByIdForUpdate (SELECT FOR UPDATE).
        when(repository.loadByIdForUpdate(marketId)).thenReturn(market);
        // Two unsettled Positions get enqueued for async settlement.
        when(positionSettlementRepository.enqueueUnsettledPositionsForMarket(marketId)).thenReturn(2);

        marketService.resolveMarket(marketId, "YES");

        // Outcome recorded; Market is RESOLUTION_PENDING (NOT RESOLVED) — payout is async.
        assertEquals(MarketStatus.RESOLUTION_PENDING, market.getStatus());
        assertEquals(Outcome.YES, market.getResolvedOutcome());

        verify(repository).saveAll(any());
        verify(positionSettlementRepository).enqueueUnsettledPositionsForMarket(marketId);
        // Work was enqueued, so we must NOT short-circuit to RESOLVED here.
        verify(repository, never()).markResolvedIfFullySettled(any());
        verify(eventPublisher).publishEvent(any(MarketResolvedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(MarketSettlementCompletedEvent.class));
    }

    @Test
    public void resolveMarket_withNoUnsettledPositions_flipsStraightToResolved() {
        String marketId = "market-empty";
        Market market = new Market(marketId, "Empty Market", "Desc");

        when(repository.loadByIdForUpdate(marketId)).thenReturn(market);
        // No unsettled Positions => zero rows enqueued.
        when(positionSettlementRepository.enqueueUnsettledPositionsForMarket(marketId)).thenReturn(0);
        when(repository.markResolvedIfFullySettled(marketId)).thenReturn(true);

        marketService.resolveMarket(marketId, "NO");

        // With no settlement work, the worker would never run for this Market, so
        // resolve flips it straight to RESOLVED and signals completion.
        verify(repository).markResolvedIfFullySettled(marketId);
        verify(eventPublisher).publishEvent(any(MarketSettlementCompletedEvent.class));
        verify(eventPublisher).publishEvent(any(MarketResolvedEvent.class));
    }

    @Test
    public void resolveMarket_unknownMarket_throws() {
        when(repository.loadByIdForUpdate("missing")).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> marketService.resolveMarket("missing", "YES"));

        verify(positionSettlementRepository, never()).enqueueUnsettledPositionsForMarket(any());
    }

    @Test
    public void getMarketById_resolvedMarket_emitsCertainties() {
        String marketId = "market-resolved-1";
        Market market = new Market(marketId, "Resolved Market", "Desc");
        // Simulate LMSR price shift (not 1.0 / 0.0), then resolve.
        market.applyTrade(Outcome.NO, 50);
        market.resolveMarket(Outcome.NO);

        when(repository.loadById(marketId)).thenReturn(market);
        when(tradeService.getTotalCostByMarketId(marketId)).thenReturn(new java.math.BigDecimal("150.00"));

        api.dto.GetAllMarket response = marketService.getMarketById(marketId);

        assertEquals(Outcome.NO, response.getResolvedOutcome());
        // resolve() lands the Market in RESOLUTION_PENDING; the DTO keys on
        // resolvedOutcome != null (not the specific status) to emit certainties.
        assertEquals(MarketStatus.RESOLUTION_PENDING, response.getStatus());

        // Even though LMSR prices are something like 0.3 / 0.7,
        // the response should emit the resolved certainty (1.0 for NO, 0.0 for YES).
        for (core.market.MarketStats stats : response.getOutcomes()) {
            if (stats.getOutcomeId() == Outcome.NO) {
                assertEquals(1.0, stats.getProbability(), 0.001);
            } else if (stats.getOutcomeId() == Outcome.YES) {
                assertEquals(0.0, stats.getProbability(), 0.001);
            }
        }

        assertEquals(new java.math.BigDecimal("150.00"), response.getTotalValue());
    }

    @Test
    public void getMarketById_missing_returnsNull() {
        when(repository.loadById(eq("nope"))).thenReturn(null);
        assertEquals(null, marketService.getMarketById("nope"));
    }
}
