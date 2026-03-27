package api.dto;

import java.math.BigDecimal;
import java.time.Instant;

import core.market.Outcome;

public record UserMarketTradeResponse(
    String tradeId,
    Outcome outcome,
    double sharesBought,
    BigDecimal cost,
    Instant tradedAt
) {}
