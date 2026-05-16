package core.ledger;

import java.math.BigDecimal;
import java.time.Instant;

public final class LedgerEntry {
    private final Long id;
    private final String userId;
    private final BigDecimal amountDelta;
    private final LedgerType type;
    private final String referenceId;
    private final String idempotencyKey;
    private final Instant createdAt;

    public LedgerEntry(Long id, String userId, BigDecimal amountDelta, LedgerType type, String referenceId,
            String idempotencyKey, Instant createdAt) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (amountDelta == null || amountDelta.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("amountDelta must not be null or zero");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId must not be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }

        this.id = id;
        this.userId = userId;
        this.amountDelta = amountDelta;
        this.type = type;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAmountDelta() {
        return amountDelta;
    }

    public LedgerType getType() {
        return type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
