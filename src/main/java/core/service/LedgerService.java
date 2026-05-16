package core.service;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.stereotype.Service;

import core.ledger.LedgerEntry;
import core.ledger.LedgerType;
import core.repository.port.LedgerRepository;
import core.trade.Trade;
import core.user.User;

/**
 * Ledger is the source of truth for user balance changes.
 *
 * <p>Money-movement entry points ({@link #recordTradeDebit},
 * {@link #recordGiftCredit}, {@link #recordSettlementCredit}) mutate
 * {@code lockedUser.balance} as part of writing the ledger entry — callers
 * (TradeEngine, SettlementEngine) must NOT touch the balance themselves.
 *
 * <p>{@link #recordStartingBalance} is a snapshot: the user already holds
 * {@link User#DEFAULT_STARTING_BALANCE} from the constructor, and the ledger
 * just records that fact. The {@code MIGRATION_BALANCE} type is owned by the
 * V9 SQL migration (no Java entry point) and behaves identically.
 *
 * <p>All entry points are idempotent on {@code idempotencyKey}. Callers MUST
 * hold a {@code SELECT ... FOR UPDATE} lock on the user row for the duration
 * of the surrounding {@code @Transactional} — otherwise two concurrent calls
 * with the same key can both pass the existence check and one will fail on
 * the unique index.
 */
@Service
public class LedgerService {
    private static final BigDecimal STARTING_BALANCE_AMOUNT = new BigDecimal("1000.00");

    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public LedgerEntry recordStartingBalance(User lockedUser) {
        requireUser(lockedUser);
        String idempotencyKey = "STARTING_BALANCE:" + lockedUser.getUserId();
        // referenceId is the userId itself — self-event, no external reference.
        return recordEntry(
                lockedUser,
                STARTING_BALANCE_AMOUNT,
                LedgerType.STARTING_BALANCE,
                lockedUser.getUserId(),
                idempotencyKey,
                false);
    }

    public LedgerEntry recordTradeDebit(Trade trade, User lockedUser) {
        requireUser(lockedUser);
        String clientRequestId = trade.getClientRequestId();
        String idempotencyKey = clientRequestId == null || clientRequestId.isBlank()
                ? "TRADE_DEBIT:" + trade.getUserId() + ":" + trade.getTradeId()
                : "BUY:" + trade.getUserId() + ":" + clientRequestId;
        return recordEntry(
                lockedUser,
                trade.getCost().negate(),
                LedgerType.TRADE_DEBIT,
                trade.getTradeId(),
                idempotencyKey,
                true);
    }

    public LedgerEntry recordGiftCredit(BigDecimal amount, User lockedUser, String referenceKey) {
        requireUser(lockedUser);
        String idempotencyKey = "GIFT_CREDIT:" + lockedUser.getUserId() + ":" + referenceKey;
        return recordEntry(
                lockedUser,
                amount,
                LedgerType.GIFT_CREDIT,
                referenceKey,
                idempotencyKey,
                true);
    }

    public LedgerEntry recordSettlementCredit(String marketId, User lockedUser, BigDecimal amount) {
        requireUser(lockedUser);
        String idempotencyKey = "SETTLEMENT_CREDIT:" + lockedUser.getUserId() + ":" + marketId;
        return recordEntry(
                lockedUser,
                amount,
                LedgerType.SETTLEMENT_CREDIT,
                marketId,
                idempotencyKey,
                true);
    }

    private void requireUser(User lockedUser) {
        if (lockedUser == null) {
            throw new IllegalArgumentException("lockedUser must not be null");
        }
    }

    private LedgerEntry recordEntry(User lockedUser, BigDecimal amountDelta, LedgerType type, String referenceId,
            String idempotencyKey, boolean applyToBalance) {
        if (lockedUser == null) {
            throw new IllegalArgumentException("lockedUser must not be null");
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

        LedgerEntry existing = ledgerRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return existing;
        }

        if (applyToBalance) {
            BigDecimal newBalance = lockedUser.getBalance().add(amountDelta);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Insufficient balance");
            }
            lockedUser.setBalance(newBalance);
        }

        LedgerEntry ledgerEntry = new LedgerEntry(
                null,
                lockedUser.getUserId(),
                amountDelta,
                type,
                referenceId,
                idempotencyKey,
                Instant.now());
        return ledgerRepository.save(ledgerEntry);
    }
}
