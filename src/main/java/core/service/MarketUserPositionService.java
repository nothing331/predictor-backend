package core.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import api.dto.UserMarketPositionResponse;
import api.dto.UserMarketTradeResponse;
import core.market.Market;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.trade.Trade;
import core.user.User;

@Service
public class MarketUserPositionService {

    private final UserService userService;
    private final TradeService tradeService;
    private final MarketRepository marketRepository;

    public MarketUserPositionService(UserService userService, TradeService tradeService,
            MarketRepository marketRepository) {
        this.userService = userService;
        this.tradeService = tradeService;
        this.marketRepository = marketRepository;
    }

    public UserMarketPositionResponse getMarketPosition(String userId, String marketId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Market market = marketRepository.loadById(marketId);
        if (market == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Market not found");
        }

        List<Trade> trades = new ArrayList<>(tradeService.getTradesByUserIdAndMarketIdOrderedDesc(userId, marketId));
        MarketTradeAggregate aggregate = new MarketTradeAggregate();
        List<UserMarketTradeResponse> tradeResponses = new ArrayList<>();

        for (Trade trade : trades) {
            aggregate.record(trade);
            tradeResponses.add(new UserMarketTradeResponse(
                    trade.getTradeId(),
                    trade.getOutcome(),
                    trade.getShareCount(),
                    trade.getCost(),
                    trade.getCreatedAt()));
        }

        BigDecimal projectedPayoutIfYes = BigDecimal.valueOf(aggregate.yesSharesHeld());
        BigDecimal projectedPayoutIfNo = BigDecimal.valueOf(aggregate.noSharesHeld());

        // Realized P/L is meaningful as soon as the outcome is known
        // (RESOLUTION_PENDING) even before the worker has paid out this user.
        // Branches on resolvedOutcome — see ADR-0003.
        BigDecimal realizedPayout = null;
        BigDecimal realizedNetPnl = null;
        if (market.getResolvedOutcome() != null) {
            double winningShares = market.getResolvedOutcome() == Outcome.YES
                    ? aggregate.yesSharesHeld()
                    : aggregate.noSharesHeld();
            realizedPayout = BigDecimal.valueOf(winningShares);
            realizedNetPnl = realizedPayout.subtract(aggregate.totalInvested());
        }

        return new UserMarketPositionResponse(
                user.getUserId(),
                market.getMarketId(),
                market.getMarketName(),
                market.getStatus(),
                market.getResolvedOutcome(),
                currentYesChance(market),
                currentNoChance(market),
                aggregate.yesSharesHeld(),
                aggregate.noSharesHeld(),
                aggregate.totalInvested(),
                aggregate.totalYesInvested(),
                aggregate.totalNoInvested(),
                aggregate.firstTradeAt(),
                aggregate.lastTradeAt(),
                projectedPayoutIfYes,
                projectedPayoutIfNo,
                realizedPayout,
                realizedNetPnl,
                aggregate.tradeCount(),
                tradeResponses);
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

    private static final class MarketTradeAggregate {
        private double yesSharesHeld;
        private double noSharesHeld;
        private BigDecimal totalInvested = BigDecimal.ZERO;
        private BigDecimal totalYesInvested = BigDecimal.ZERO;
        private BigDecimal totalNoInvested = BigDecimal.ZERO;
        private Instant firstTradeAt;
        private Instant lastTradeAt;
        private int tradeCount;

        void record(Trade trade) {
            tradeCount++;
            totalInvested = totalInvested.add(trade.getCost());

            if (trade.getOutcome() == Outcome.YES) {
                yesSharesHeld += trade.getShareCount();
                totalYesInvested = totalYesInvested.add(trade.getCost());
            } else {
                noSharesHeld += trade.getShareCount();
                totalNoInvested = totalNoInvested.add(trade.getCost());
            }

            if (firstTradeAt == null || trade.getCreatedAt().isBefore(firstTradeAt)) {
                firstTradeAt = trade.getCreatedAt();
            }
            if (lastTradeAt == null || trade.getCreatedAt().isAfter(lastTradeAt)) {
                lastTradeAt = trade.getCreatedAt();
            }
        }

        double yesSharesHeld() {
            return yesSharesHeld;
        }

        double noSharesHeld() {
            return noSharesHeld;
        }

        BigDecimal totalInvested() {
            return totalInvested;
        }

        BigDecimal totalYesInvested() {
            return totalYesInvested;
        }

        BigDecimal totalNoInvested() {
            return totalNoInvested;
        }

        Instant firstTradeAt() {
            return firstTradeAt;
        }

        Instant lastTradeAt() {
            return lastTradeAt;
        }

        int tradeCount() {
            return tradeCount;
        }
    }
}
