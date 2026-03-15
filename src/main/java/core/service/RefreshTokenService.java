package core.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import core.repository.adapter.db.JpaRefreshTokenRepository;
import core.repository.adapter.db.JpaUserRepository;
import core.user.User;
import db.entity.RefreshToken;
import db.entity.UserEntity;

@Service
public class RefreshTokenService {

    @Autowired
    private JpaRefreshTokenRepository repo;

    @Autowired
    private JpaUserRepository userRepo;

    @Value("${app.jwt-refresh-expiry-day}")
    private int expiryDays;

    public String create(User user) {
        String raw = UUID.randomUUID().toString();
        UserEntity userEntity = userRepo.findById(user.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        RefreshToken entity = new RefreshToken();
        entity.setTokenHash(hash(raw));
        entity.setUser(userEntity);
        entity.setExpiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS));
        entity.setRevoked(false);
        repo.save(entity);
        return raw;
    }

    public User validate(String raw) {
        RefreshToken token = repo.findByTokenHash(hash(raw))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        
        UserEntity userEntity = token.getUser();
        return new User(userEntity.getUserId(), userEntity.getBalance());
    }

    public void rotate(String raw) {
        repo.findByTokenHash(hash(raw)).ifPresent(t -> {
            t.setRevoked(true);
            repo.save(t);
        });
    }

    public void revoke(String raw) {
        rotate(raw);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(raw.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
