package api.dto.auth;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Frontend-safe representation of a logged-in user.
 */
public record AuthUserResponse(
    String userId,
    String email,
    String name,
    String pictureUrl,
    BigDecimal balance,
    String role,
    boolean giftAvailable,
    Instant nextGiftAt
) {}
