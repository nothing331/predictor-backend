package db.entity;

import java.time.Instant;

import core.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens", schema = "market")
public class RefreshToken {
    @Id 
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @jakarta.persistence.Column(name = "token_id")
    private Long id;
    
    @jakarta.persistence.Column(name = "token_hash")
    private String tokenHash;     // store SHA-256 hash, not raw value
    
    @ManyToOne
    @jakarta.persistence.JoinColumn(name = "user_id")
    private UserEntity user;
    
    @jakarta.persistence.Column(name = "expires_at")
    private Instant expiresAt;
    
    @jakarta.persistence.Column(name = "revoked_at")
    private Instant revokedAt;
    
    @jakarta.persistence.Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void setRevoked(boolean revoked) {
        this.revokedAt = revoked ? Instant.now() : null;
    }
}