package api.dto.auth;

import java.math.BigDecimal;

/**
 * Frontend-safe representation of a logged-in user.
 */
public record AuthUserResponse(
    String userId,
    String email,
    String name,
    String pictureUrl,
    BigDecimal balance
) {}
