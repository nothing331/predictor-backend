package core.user;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class User {

    public static final BigDecimal DEFAULT_STARTING_BALANCE = new BigDecimal("1000.00");

    private final String userId;
    private BigDecimal balance;
    private final Map<String, Position> positions;

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
}
