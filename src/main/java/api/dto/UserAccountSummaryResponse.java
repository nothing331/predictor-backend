package api.dto;

import java.math.BigDecimal;
import java.util.List;

public record UserAccountSummaryResponse(
    String userId,
    BigDecimal availableBalance,
    List<UserRecentMarketSummary> recentMarkets
) {}
