package core.repository.adapter.db;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import core.ledger.LedgerType;
import db.entity.LedgerEntryEntity;

@Repository
public interface JpaLedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {
    Optional<LedgerEntryEntity> findByIdempotencyKey(String idempotencyKey);

    List<LedgerEntryEntity> findByUserIdOrderByCreatedAtDescIdDesc(String userId);

    List<LedgerEntryEntity> findByTypeAndReferenceId(LedgerType type, String referenceId);

    @Query("SELECT COALESCE(SUM(l.amountDelta), 0) FROM LedgerEntryEntity l WHERE l.userId = :userId")
    BigDecimal sumAmountDeltaByUserId(@Param("userId") String userId);
}
