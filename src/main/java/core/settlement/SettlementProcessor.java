package core.settlement;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import core.market.Market;
import core.market.Outcome;
import core.repository.port.MarketRepository;
import core.repository.port.PositionSettlementRepository;
import core.service.LedgerService;
import core.service.UserService;
import core.user.Position;
import core.user.User;

/**
 * Settlement worker's per-row transaction.
 *
 * <p>Lock order: {@code position_settlements row (SKIP LOCKED) -> User (FOR UPDATE)}.
 * The Market row is read without a lock — by the time a row reaches this method,
 * Resolution has long since committed and the Market's {@code resolved_outcome}
 * is immutable. Taking a Market lock here would risk deadlocks against buys
 * (see {@code docs/adr/0004-lock-order-market-then-user.md}).
 *
 * <p>Failure classification:
 * <ul>
 *   <li>{@link TransientSettlementException} — bookkeeper bumps {@code attempts}
 *       and schedules a retry.</li>
 *   <li>{@link TerminalSettlementException} — bookkeeper marks the row
 *       {@code FAILED} and the Market {@code SETTLEMENT_FAILED}.</li>
 * </ul>
 */
@Service
public class SettlementProcessor {

    private final PositionSettlementRepository positionSettlementRepository;
    private final MarketRepository marketRepository;
    private final UserService userService;
    private final LedgerService ledgerService;
    private final SettlementEngine settlementEngine;

    public SettlementProcessor(PositionSettlementRepository positionSettlementRepository,
            MarketRepository marketRepository, UserService userService,
            LedgerService ledgerService, SettlementEngine settlementEngine) {
        this.positionSettlementRepository = positionSettlementRepository;
        this.marketRepository = marketRepository;
        this.userService = userService;
        this.ledgerService = ledgerService;
        this.settlementEngine = settlementEngine;
    }

    public sealed interface ProcessOutcome permits Empty, Settled {
        static Empty empty() { return new Empty(); }
    }
    public record Empty() implements ProcessOutcome {}
    public record Settled(String marketId, String userId) implements ProcessOutcome {}

    /**
     * Claim one row and settle one Position in a single transaction. Returns
     * {@link Empty} if no row was claimable (no work to do this tick).
     *
     * @throws TransientSettlementException to signal a retryable failure
     * @throws TerminalSettlementException  to signal a fail-fast failure
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessOutcome claimAndProcessOne() {
        Optional<PositionSettlement> claimed = positionSettlementRepository.claimNextPending();
        if (claimed.isEmpty()) {
            return ProcessOutcome.empty();
        }
        PositionSettlement row = claimed.get();
        String marketId = row.getMarketId();
        String userId = row.getUserId();

        // Lock the User row FOR UPDATE — required by LedgerService for balance writes.
        User user;
        try {
            user = userService.getUserByIdForUpdate(userId);
        } catch (RuntimeException e) {
            throw new TransientSettlementException(marketId, userId, "user lock failed", e);
        }
        if (user == null) {
            // FK should prevent this; if it happens, the row is unworkable.
            throw new TerminalSettlementException(marketId, userId,
                    "user not found: " + userId, null);
        }

        Market market = marketRepository.loadById(marketId);
        if (market == null) {
            throw new TerminalSettlementException(marketId, userId,
                    "market not found: " + marketId, null);
        }
        Outcome winner = market.getResolvedOutcome();
        if (winner == null) {
            // Invariant violation: the Market must have an outcome by the time
            // a settlement row exists. See Market.validate().
            throw new TerminalSettlementException(marketId, userId,
                    "market has no resolved outcome: " + marketId, null);
        }

        Position position = user.getPosition(marketId);
        if (position == null) {
            // Position was deleted out from under us, or never existed.
            throw new TerminalSettlementException(marketId, userId,
                    "no position for user " + userId + " in market " + marketId, null);
        }

        // Idempotency at the position level: if the Position is already settled
        // (e.g. a previous worker committed but its delete somehow lost the row),
        // we just clean up the queue row. Ledger collision would have the same
        // effect via SETTLEMENT_CREDIT idempotency key, but skipping the ledger
        // call here also avoids re-reading the entry unnecessarily.
        if (!position.isSettled()) {
            BigDecimal payout = computePayout(position, winner);

            try {
                if (payout.compareTo(BigDecimal.ZERO) > 0) {
                    ledgerService.recordSettlementCredit(marketId, user, payout);
                }
                settlementEngine.settleUser(user, market);
                userService.saveUser(user);
            } catch (RuntimeException e) {
                // Treat domain/ledger exceptions as transient by default — the
                // bookkeeper will increment attempts and push out next_run_at.
                // Truly unrecoverable cases (e.g. negative payout) are caught
                // by the invariant checks above.
                throw new TransientSettlementException(marketId, userId,
                        "settle/ledger failed: " + e.getMessage(), e);
            }
        }

        boolean deleted = positionSettlementRepository.deleteByKey(marketId, userId);
        if (!deleted) {
            // Another worker beat us to it — extremely rare given SKIP LOCKED,
            // but harmless. Treat as success.
        }

        return new Settled(marketId, userId);
    }

    private BigDecimal computePayout(Position position, Outcome winner) {
        double shares = (winner == Outcome.YES) ? position.getYesShares() : position.getNoShares();
        return BigDecimal.valueOf(shares);
    }
}
