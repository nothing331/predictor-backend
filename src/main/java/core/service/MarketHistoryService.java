package core.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import api.dto.MarketHistoryPoint;
import api.dto.MarketHistoryResponse;
import core.lmsr.PricingEngine;
import core.market.Market;
import core.market.MarketStatus;
import core.market.Outcome;
import core.store.MarketStore;
import core.trade.Trade;

@Service
public class MarketHistoryService {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final MarketStore marketStore;
    private final TradeService tradeService;

    public MarketHistoryService(MarketStore marketStore, TradeService tradeService) {
        this.marketStore = marketStore;
        this.tradeService = tradeService;
    }

    public MarketHistoryResponse getHistory(String marketId, Instant from, Instant to, int limit) {
        if (marketId == null || marketId.isBlank()) {
            throw new IllegalArgumentException("marketId must not be blank");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        Market market = marketStore.get(marketId);
        if (market == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Market not found");
        }

        int effectiveLimit = normalizeLimit(limit);
        List<Trade> orderedTrades = new ArrayList<>(tradeService.getTradesByMarketIdOrdered(marketId));
        List<MarketHistoryPoint> points = buildPoints(market, orderedTrades);
        List<MarketHistoryPoint> filteredPoints = filterPoints(points, from, to, effectiveLimit);

        return new MarketHistoryResponse(market.getMarketId(), market.getStatus(), filteredPoints);
    }

    private List<MarketHistoryPoint> buildPoints(Market market, Collection<Trade> orderedTrades) {
        List<MarketHistoryPoint> points = new ArrayList<>();
        double qYes = 0.0;
        double qNo = 0.0;
        double liquidity = market.getLiquidity();

        Instant initialTimestamp = market.getCreatedAt();
        if (initialTimestamp == null && !orderedTrades.isEmpty()) {
            initialTimestamp = orderedTrades.iterator().next().getCreatedAt();
        }
        if (initialTimestamp != null) {
            points.add(MarketHistoryPoint.initial(initialTimestamp, 0.5, 0.5));
        }

        for (Trade trade : orderedTrades) {
            if (trade.getOutcome() == Outcome.YES) {
                qYes += trade.getShareCount();
            } else {
                qNo += trade.getShareCount();
            }

            double yesProbability = PricingEngine.displayYesPrice(qYes, qNo, liquidity);
            double noProbability = PricingEngine.displayNoPrice(qYes, qNo, liquidity);

            points.add(MarketHistoryPoint.trade(
                    trade.getCreatedAt(),
                    yesProbability,
                    noProbability,
                    trade.getTradeId(),
                    trade.getOutcome(),
                    trade.getShareCount(),
                    trade.getCost()));
        }

        if (market.getStatus() == MarketStatus.RESOLVED && market.getResolvedOutcome() != null
                && market.getResolvedAt() != null) {
            boolean yesWon = market.getResolvedOutcome() == Outcome.YES;
            points.add(MarketHistoryPoint.resolution(
                    market.getResolvedAt(),
                    yesWon ? 1.0 : 0.0,
                    yesWon ? 0.0 : 1.0));
        }

        return points;
    }

    private List<MarketHistoryPoint> filterPoints(List<MarketHistoryPoint> points, Instant from, Instant to, int limit) {
        List<MarketHistoryPoint> filtered = points.stream()
                .filter(point -> from == null || !point.getTimestamp().isBefore(from))
                .filter(point -> to == null || !point.getTimestamp().isAfter(to))
                .toList();

        if (filtered.size() <= limit) {
            return filtered;
        }

        return filtered.subList(filtered.size() - limit, filtered.size());
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
