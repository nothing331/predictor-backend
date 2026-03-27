package api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import core.market.MarketStatus;
import core.market.Outcome;

public record UserRecentMarketSummary(
    String marketId,
    String marketName,
    MarketStatus marketStatus,
    Instant lastTradedAt,
    Outcome resolvedOutcome,
    double userYesShares,
    double userNoShares,
    double currentYesChance,
    double currentNoChance,
    BigDecimal projectedPayoutIfYes,
    BigDecimal projectedPayoutIfNo
) {}
