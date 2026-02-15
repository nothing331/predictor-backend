package core.user;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class User {

    public static final BigDecimal DEFAULT_STARTING_BALANCE = new BigDecimal("1000.00");

    private String userId;
    private BigDecimal balance;
    private Map<String, Position> positions;

    /**
     * Default constructor for frameworks (e.g., Jackson).
     */
    protected User() {
        this.positions = new HashMap<>();
    }

    public User(String userId) {
        this(userId, DEFAULT_STARTING_BALANCE);
    }

    public User(String userId, BigDecimal balance) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("userId cannot be null or empty");
        }
        if (balance == null) {
            throw new IllegalArgumentException("balance cannot be null");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("balance cannot be negative");
        }

        this.userId = userId;
        this.balance = balance;
        this.positions = new HashMap<>();
    }

    // ======================== GETTERS ========================

    public String getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Map<String, Position> getPositions() {
        return Collections.unmodifiableMap(positions);
    }

    public Position getPosition(String marketId) {
        return positions.get(marketId);
    }

    public Position getOrCreatePosition(String marketId) {
        return positions.computeIfAbsent(marketId, Position::new);
    }

    /**
     * Set the user's balance to a new value.
     * 
     * @param newBalance The new balance (must not be null or negative)
     */
    public void setBalance(BigDecimal newBalance) {
        if (newBalance == null) {
            throw new IllegalArgumentException("balance cannot be null");
        }
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("balance cannot be negative");
        }
        this.balance = newBalance;
    }

    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("User userId cannot be null or empty");
        }
        if (balance == null) {
            throw new IllegalStateException("User balance cannot be null for user: " + userId);
        }

        // Validate positions map structure: marketId -> Position
        if (positions != null) {
            for (Map.Entry<String, Position> entry : positions.entrySet()) {
                String marketId = entry.getKey();
                Position position = entry.getValue();

                // Validate marketId is not null or empty
                if (marketId == null || marketId.trim().isEmpty()) {
                    throw new IllegalStateException(
                            "Position marketId cannot be null or empty for user: " + userId);
                }

                // Validate position is not null
                if (position == null) {
                    throw new IllegalStateException(
                            "Position cannot be null for marketId: " + marketId + " in user: " + userId);
                }

                // Validate that the position's marketId matches the map key
                if (!marketId.equals(position.getMarketId())) {
                    throw new IllegalStateException("Position marketId mismatch: map key is " + marketId +
                            " but position.getMarketId() is " + position.getMarketId() + " for user: "
                            + userId);
                }
            }
        }
    }
}
