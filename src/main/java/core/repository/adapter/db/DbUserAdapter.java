package core.repository.adapter.db;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import core.user.Position;
import core.user.User;
import core.repository.port.UserRepository;
import db.entity.PositionEntity;
import db.entity.UserEntity;

@Repository("userDbAdapter")
public class DbUserAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final JpaPositionRepository jpaPositionRepository;

    public DbUserAdapter(JpaUserRepository jpaUserRepository, JpaPositionRepository jpaPositionRepository) {
        this.jpaUserRepository = jpaUserRepository;
        this.jpaPositionRepository = jpaPositionRepository;
    }

    @Override
    public void saveAll(Collection<User> users) {
        for (User user : users) {
            saveUser(user);
        }
    }

    private void saveUser(User user) {
        UserEntity entity = toEntity(user);
        jpaUserRepository.save(entity);

        // Handle positions
        // This is tricky. In a real app we'd diff.
        // For now, simpler to load existing, map, save/update.
        // Or just save all positions from User.
        // However, User.getPositions() returns a Map<MarketId, Position>.

        // Strategy:
        // 1. Get existing positions from DB for this user.
        // 2. Update them or create new ones.
        // 3. Delete ones not in User (if necessary, but User object usually holds all).

        // Simpler implementation: Just save/update all current positions.
        Map<String, Position> positions = user.getPositions();
        List<PositionEntity> existingEntities = jpaPositionRepository.findByUserId(user.getUserId());

        for (Position pos : positions.values()) {
            PositionEntity matchingEntity = existingEntities.stream()
                    .filter(e -> e.getMarketId().equals(pos.getMarketId()))
                    .findFirst()
                    .orElse(null);

            if (matchingEntity != null) {
                matchingEntity.setYesShares(BigDecimal.valueOf(pos.getYesShares()));
                matchingEntity.setNoShares(BigDecimal.valueOf(pos.getNoShares()));
                matchingEntity.setSettled(pos.isSettled());
                jpaPositionRepository.save(matchingEntity);
            } else {
                PositionEntity newEntity = new PositionEntity(
                        null, // auto-increment
                        user.getUserId(),
                        pos.getMarketId(),
                        BigDecimal.valueOf(pos.getYesShares()),
                        BigDecimal.valueOf(pos.getNoShares()),
                        pos.isSettled());
                jpaPositionRepository.save(newEntity);
            }
        }
    }

    @Override
    public Collection<User> loadAll() {
        return jpaUserRepository.findAll().stream()
                .map(this::toDomainAndHydrate)
                .collect(Collectors.toList());
    }

    @Override
    public User loadById(String userId) {
        return jpaUserRepository.findById(userId)
                .map(this::toDomainAndHydrate)
                .orElse(null);
    }

    @Override
    public User loadByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(this::toDomainAndHydrate)
                .orElse(null);
    }

    @Override
    public User loadByUserName(String userName) {
        return jpaUserRepository.findByUserName(userName)
                .map(this::toDomainAndHydrate)
                .orElse(null);
    }

    private UserEntity toEntity(User user) {
        // UserEntity has email, createdAt, updatedAt which are not in User.
        // We might lose data if we don't load first.
        // But for saveAll, usually we modify existing.
        // If it's a new user, we need defaults.

        return jpaUserRepository.findById(user.getUserId())
                .map(existing -> {
                    existing.setBalance(user.getBalance());
                    existing.setUserName(user.getDisplayName());
                    existing.setEmail(user.getEmail());
                    existing.setGoogleSub(user.getGoogleSub());
                    existing.setPictureUrl(user.getPictureUrl());
                    existing.setEmailVerified(user.isEmailVerified());
                    existing.setPasswordHash(user.getPasswordHash());
                    existing.setRole(user.getRole() != null ? user.getRole().name() : "USER");
                    return existing;
                })
                .orElseGet(() -> {
                    UserEntity entity = new UserEntity(
                        user.getUserId(),
                        user.getDisplayName(),
                        user.getEmail(),
                        user.getBalance(),
                        new Timestamp(System.currentTimeMillis()),
                        new Timestamp(System.currentTimeMillis()));
                    entity.setGoogleSub(user.getGoogleSub());
                    entity.setPictureUrl(user.getPictureUrl());
                    entity.setEmailVerified(user.isEmailVerified());
                    entity.setPasswordHash(user.getPasswordHash());
                    entity.setRole(user.getRole() != null ? user.getRole().name() : "USER");
                    return entity;
                });
    }

    private User toDomainAndHydrate(UserEntity entity) {
        User user = new User(entity.getUserId(), entity.getBalance());
        user.setEmail(entity.getEmail());
        user.setDisplayName(entity.getUserName());
        user.setGoogleSub(entity.getGoogleSub());
        user.setPictureUrl(entity.getPictureUrl());
        user.setEmailVerified(entity.isEmailVerified());
        user.setPasswordHash(entity.getPasswordHash());
        user.setRole(entity.getRole() != null ? core.user.UserRole.valueOf(entity.getRole()) : core.user.UserRole.USER);

        // Hydrate positions
        List<PositionEntity> posEntities = jpaPositionRepository.findByUserId(entity.getUserId());

        // Access 'positions' map in User via reflection or just use
        // getOrCreatePosition?
        // User has getOrCreatePosition(marketId).
        for (PositionEntity posEntity : posEntities) {
            Position pos = user.getOrCreatePosition(posEntity.getMarketId());
            pos.setYesShares(posEntity.getYesShares().doubleValue());
            pos.setNoShares(posEntity.getNoShares().doubleValue());
            if (posEntity.isSettled()) {
                pos.markAsSettled();
            }
        }

        return user;
    }
}
