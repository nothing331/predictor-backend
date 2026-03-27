package api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import core.market.MarketStatus;
import core.market.Outcome;

public record UserMarketPositionResponse(
    String userId,
    String marketId,
    String marketName,
    MarketStatus marketStatus,
    Outcome resolvedOutcome,
    double currentYesChance,
    double currentNoChance,
    double yesSharesHeld,
    double noSharesHeld,
    BigDecimal totalInvested,
    BigDecimal totalYesInvested,
    BigDecimal totalNoInvested,
    Instant firstTradeAt,
    Instant lastTradeAt,
    BigDecimal projectedPayoutIfYes,
    BigDecimal projectedPayoutIfNo,
    BigDecimal realizedPayout,
    BigDecimal realizedNetPnl,
    int tradeCount,
    List<UserMarketTradeResponse> trades
) {}
