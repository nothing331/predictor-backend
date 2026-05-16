package db.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

import core.ledger.LedgerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "ledger_entries", schema = "market")
public class LedgerEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private String userId;

    @Column(nullable = false, name = "amount_delta", precision = 20, scale = 8)
    private BigDecimal amountDelta;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private LedgerType type;

    @Column(nullable = false, name = "reference_id")
    private String referenceId;

    @Column(nullable = false, name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(nullable = false, name = "created_at")
    private Timestamp createdAt;

    protected LedgerEntryEntity() {
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }
    }

    public LedgerEntryEntity(Long id, String userId, BigDecimal amountDelta, LedgerType type, String referenceId,
            String idempotencyKey, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.amountDelta = amountDelta;
        this.type = type;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmountDelta() {
        return amountDelta;
    }

    public void setAmountDelta(BigDecimal amountDelta) {
        this.amountDelta = amountDelta;
    }

    public LedgerType getType() {
        return type;
    }

    public void setType(LedgerType type) {
        this.type = type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
