package core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import core.ledger.LedgerEntry;
import core.ledger.LedgerType;
import core.market.Outcome;
import core.repository.port.LedgerRepository;
import core.trade.Trade;
import core.user.User;

public class LedgerServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ledgerService = new LedgerService(ledgerRepository);
        when(ledgerRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ====================================================================
    // recordTradeDebit
    // ====================================================================

    @Test
    @DisplayName("recordTradeDebit with clientRequestId debits balance and uses BUY:<userId>:<clientRequestId> key")
    void testRecordTradeDebit_withClientRequestId() {
        User user = new User("user-1", new BigDecimal("100.00"));
        Trade trade = new Trade("trade-1", "user-1", "market-1", Outcome.YES, 10.0,
                new BigDecimal("30.00"), Instant.parse("2026-05-15T10:00:00Z"), "req-1");

        LedgerEntry entry = ledgerService.recordTradeDebit(trade, user);

        assertEquals(new BigDecimal("70.00"), user.getBalance(), "balance must be debited by cost");
        assertEquals(new BigDecimal("-30.00"), entry.getAmountDelta());
        assertEquals(LedgerType.TRADE_DEBIT, entry.getType());
        assertEquals("trade-1", entry.getReferenceId());
        assertEquals("BUY:user-1:req-1", entry.getIdempotencyKey());
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("recordTradeDebit without clientRequestId falls back to TRADE_DEBIT:<userId>:<tradeId> key")
    void testRecordTradeDebit_withoutClientRequestId() {
        User user = new User("user-1", new BigDecimal("100.00"));
        Trade trade = new Trade("trade-7", "user-1", "market-1", Outcome.YES, 10.0,
                new BigDecimal("30.00"), Instant.parse("2026-05-15T10:00:00Z"), null);

        LedgerEntry entry = ledgerService.recordTradeDebit(trade, user);

        assertEquals("TRADE_DEBIT:user-1:trade-7", entry.getIdempotencyKey());
        assertEquals(new BigDecimal("70.00"), user.getBalance());
    }

    @Test
    @DisplayName("recordTradeDebit is idempotent — second call returns existing entry without re-debiting balance")
    void testRecordTradeDebit_idempotentRetry() {
        User user = new User("user-1", new BigDecimal("70.00"));
        Trade trade = new Trade("trade-1", "user-1", "market-1", Outcome.YES, 10.0,
                new BigDecimal("30.00"), Instant.parse("2026-05-15T10:00:00Z"), "req-1");

        LedgerEntry existing = new LedgerEntry(42L, "user-1", new BigDecimal("-30.00"),
                LedgerType.TRADE_DEBIT, "trade-1", "BUY:user-1:req-1",
                Instant.parse("2026-05-15T09:59:00Z"));
        when(ledgerRepository.findByIdempotencyKey("BUY:user-1:req-1")).thenReturn(Optional.of(existing));

        LedgerEntry result = ledgerService.recordTradeDebit(trade, user);

        assertSame(existing, result, "should return the existing entry verbatim");
        assertEquals(new BigDecimal("70.00"), user.getBalance(), "balance must not be debited a second time");
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("recordTradeDebit throws when cost exceeds balance — User.setBalance never called")
    void testRecordTradeDebit_insufficientBalance() {
        User user = new User("user-1", new BigDecimal("5.00"));
        Trade trade = new Trade("trade-1", "user-1", "market-1", Outcome.YES, 10.0,
                new BigDecimal("30.00"), Instant.parse("2026-05-15T10:00:00Z"), "req-1");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> ledgerService.recordTradeDebit(trade, user));
        assertEquals("Insufficient balance", thrown.getMessage());
        assertEquals(new BigDecimal("5.00"), user.getBalance(), "balance must be untouched on failure");
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    // ====================================================================
    // recordGiftCredit
    // ====================================================================

    @Test
    @DisplayName("recordGiftCredit credits balance and uses GIFT_CREDIT:<userId>:<referenceKey> key")
    void testRecordGiftCredit_happyPath() {
        User user = new User("user-1", new BigDecimal("100.00"));

        LedgerEntry entry = ledgerService.recordGiftCredit(
                new BigDecimal("500.00"), user, "43200:1747267200");

        assertEquals(new BigDecimal("600.00"), user.getBalance());
        assertEquals(new BigDecimal("500.00"), entry.getAmountDelta());
        assertEquals(LedgerType.GIFT_CREDIT, entry.getType());
        assertEquals("43200:1747267200", entry.getReferenceId());
        assertEquals("GIFT_CREDIT:user-1:43200:1747267200", entry.getIdempotencyKey());
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("recordGiftCredit is idempotent — second claim in same window does not credit twice")
    void testRecordGiftCredit_idempotentRetry() {
        User user = new User("user-1", new BigDecimal("600.00"));

        LedgerEntry existing = new LedgerEntry(7L, "user-1", new BigDecimal("500.00"),
                LedgerType.GIFT_CREDIT, "43200:1747267200",
                "GIFT_CREDIT:user-1:43200:1747267200", Instant.parse("2026-05-14T22:00:00Z"));
        when(ledgerRepository.findByIdempotencyKey("GIFT_CREDIT:user-1:43200:1747267200"))
                .thenReturn(Optional.of(existing));

        LedgerEntry result = ledgerService.recordGiftCredit(
                new BigDecimal("500.00"), user, "43200:1747267200");

        assertSame(existing, result);
        assertEquals(new BigDecimal("600.00"), user.getBalance(), "balance must not be credited twice");
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    // ====================================================================
    // recordSettlementCredit
    // ====================================================================

    @Test
    @DisplayName("recordSettlementCredit credits balance and uses SETTLEMENT_CREDIT:<userId>:<marketId> key")
    void testRecordSettlementCredit_happyPath() {
        User user = new User("user-1", new BigDecimal("100.00"));

        LedgerEntry entry = ledgerService.recordSettlementCredit("market-1", user, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), user.getBalance());
        assertEquals(new BigDecimal("50.00"), entry.getAmountDelta());
        assertEquals(LedgerType.SETTLEMENT_CREDIT, entry.getType());
        assertEquals("market-1", entry.getReferenceId());
        assertEquals("SETTLEMENT_CREDIT:user-1:market-1", entry.getIdempotencyKey());
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("recordSettlementCredit is idempotent — re-resolving same market does not double-pay")
    void testRecordSettlementCredit_idempotentRetry() {
        User user = new User("user-1", new BigDecimal("150.00"));

        LedgerEntry existing = new LedgerEntry(11L, "user-1", new BigDecimal("50.00"),
                LedgerType.SETTLEMENT_CREDIT, "market-1", "SETTLEMENT_CREDIT:user-1:market-1",
                Instant.parse("2026-05-15T11:00:00Z"));
        when(ledgerRepository.findByIdempotencyKey("SETTLEMENT_CREDIT:user-1:market-1"))
                .thenReturn(Optional.of(existing));

        LedgerEntry result = ledgerService.recordSettlementCredit("market-1", user, new BigDecimal("50.00"));

        assertSame(existing, result);
        assertEquals(new BigDecimal("150.00"), user.getBalance());
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    // ====================================================================
    // recordStartingBalance — snapshot mode (does NOT mutate balance)
    // ====================================================================

    @Test
    @DisplayName("recordStartingBalance writes 1000.00 snapshot entry without mutating balance")
    void testRecordStartingBalance_snapshotDoesNotMutateBalance() {
        // User constructor already sets DEFAULT_STARTING_BALANCE = 1000.00.
        User user = new User("user-1");
        BigDecimal balanceBefore = user.getBalance();

        LedgerEntry entry = ledgerService.recordStartingBalance(user);

        assertEquals(balanceBefore, user.getBalance(),
                "STARTING_BALANCE is a snapshot — balance must be untouched");
        assertEquals(new BigDecimal("1000.00"), entry.getAmountDelta());
        assertEquals(LedgerType.STARTING_BALANCE, entry.getType());
        assertEquals("user-1", entry.getReferenceId(), "referenceId is the userId itself (self-event)");
        assertEquals("STARTING_BALANCE:user-1", entry.getIdempotencyKey());
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("recordStartingBalance is idempotent — addUser retry returns existing without re-writing")
    void testRecordStartingBalance_idempotentRetry() {
        User user = new User("user-1");

        LedgerEntry existing = new LedgerEntry(1L, "user-1", new BigDecimal("1000.00"),
                LedgerType.STARTING_BALANCE, "user-1", "STARTING_BALANCE:user-1",
                Instant.parse("2026-05-15T00:00:00Z"));
        when(ledgerRepository.findByIdempotencyKey("STARTING_BALANCE:user-1"))
                .thenReturn(Optional.of(existing));

        LedgerEntry result = ledgerService.recordStartingBalance(user);

        assertSame(existing, result);
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    // ====================================================================
    // Validation
    // ====================================================================

    @Test
    @DisplayName("recordGiftCredit throws on null lockedUser")
    void testRecordGiftCredit_nullUserThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.recordGiftCredit(new BigDecimal("500.00"), null, "43200:1747267200"));
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("recordGiftCredit throws on null amountDelta")
    void testRecordGiftCredit_nullAmountThrows() {
        User user = new User("user-1");
        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.recordGiftCredit(null, user, "43200:1747267200"));
    }

    @Test
    @DisplayName("recordGiftCredit throws on zero amountDelta (no silent no-op)")
    void testRecordGiftCredit_zeroAmountThrows() {
        User user = new User("user-1");
        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.recordGiftCredit(BigDecimal.ZERO, user, "43200:1747267200"));
        verify(ledgerRepository, never()).save(any(LedgerEntry.class));
    }

    @Test
    @DisplayName("recordGiftCredit throws on blank referenceKey")
    void testRecordGiftCredit_blankReferenceThrows() {
        User user = new User("user-1");
        assertThrows(IllegalArgumentException.class,
                () -> ledgerService.recordGiftCredit(new BigDecimal("500.00"), user, "   "));
    }

    // ====================================================================
    // Persistence shape
    // ====================================================================

    @Test
    @DisplayName("Saved LedgerEntry carries the locked user's userId, not the trade.userId field separately")
    void testRecordTradeDebit_savedEntryShape() {
        User user = new User("user-1", new BigDecimal("100.00"));
        Trade trade = new Trade("trade-1", "user-1", "market-1", Outcome.YES, 10.0,
                new BigDecimal("30.00"), Instant.parse("2026-05-15T10:00:00Z"), "req-1");

        ledgerService.recordTradeDebit(trade, user);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerRepository, times(1)).save(captor.capture());
        LedgerEntry saved = captor.getValue();

        assertNotNull(saved.getCreatedAt(), "createdAt must be set at save time");
        assertEquals("user-1", saved.getUserId());
        assertEquals(new BigDecimal("-30.00"), saved.getAmountDelta());
        assertEquals(LedgerType.TRADE_DEBIT, saved.getType());
        assertEquals("trade-1", saved.getReferenceId());
        assertEquals("BUY:user-1:req-1", saved.getIdempotencyKey());
    }
}
