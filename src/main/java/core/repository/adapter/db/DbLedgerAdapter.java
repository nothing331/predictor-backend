package core.repository.adapter.db;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import core.ledger.LedgerEntry;
import core.ledger.LedgerType;
import core.repository.port.LedgerRepository;
import db.entity.LedgerEntryEntity;

@Repository("ledgerDbAdapter")
public class DbLedgerAdapter implements LedgerRepository {
    private final JpaLedgerEntryRepository jpaLedgerEntryRepository;

    public DbLedgerAdapter(JpaLedgerEntryRepository jpaLedgerEntryRepository) {
        this.jpaLedgerEntryRepository = jpaLedgerEntryRepository;
    }

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        return toDomain(jpaLedgerEntryRepository.save(toEntity(entry)));
    }

    @Override
    public Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey) {
        return jpaLedgerEntryRepository.findByIdempotencyKey(idempotencyKey)
                .map(this::toDomain);
    }

    @Override
    public Collection<LedgerEntry> loadByUserIdOrderedDesc(String userId) {
        return jpaLedgerEntryRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Collection<LedgerEntry> loadByTypeAndReferenceId(LedgerType type, String referenceId) {
        return jpaLedgerEntryRepository.findByTypeAndReferenceId(type, referenceId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumAmountDeltaByUserId(String userId) {
        return jpaLedgerEntryRepository.sumAmountDeltaByUserId(userId);
    }

    private LedgerEntryEntity toEntity(LedgerEntry entry) {
        return new LedgerEntryEntity(
                entry.getId(),
                entry.getUserId(),
                entry.getAmountDelta(),
                entry.getType(),
                entry.getReferenceId(),
                entry.getIdempotencyKey(),
                Timestamp.from(entry.getCreatedAt()));
    }

    private LedgerEntry toDomain(LedgerEntryEntity entity) {
        return new LedgerEntry(
                entity.getId(),
                entity.getUserId(),
                entity.getAmountDelta(),
                entity.getType(),
                entity.getReferenceId(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt().toInstant());
    }
}
