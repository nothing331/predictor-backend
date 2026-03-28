package api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record GiftClaimResponse(
    BigDecimal balance,
    BigDecimal claimedAmount,
    boolean claimed,
    Instant lastClaimedAt,
    Instant nextGiftAt,
    boolean giftAvailable
) {}
