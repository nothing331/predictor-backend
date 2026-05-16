package core.repository.port;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import core.ledger.LedgerEntry;
import core.ledger.LedgerType;

public interface LedgerRepository {
    LedgerEntry save(LedgerEntry entry);

    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    Collection<LedgerEntry> loadByUserIdOrderedDesc(String userId);

    Collection<LedgerEntry> loadByTypeAndReferenceId(LedgerType type, String referenceId);

    BigDecimal sumAmountDeltaByUserId(String userId);
}
