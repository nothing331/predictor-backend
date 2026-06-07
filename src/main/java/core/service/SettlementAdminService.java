package core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import core.event.MarketSettlementRetryRequestedEvent;
import core.repository.port.MarketRepository;
import core.repository.port.PositionSettlementRepository;

/**
 * Admin-only operations on the settlement pipeline. Currently just one:
 * retrying a Market that landed in {@code SETTLEMENT_FAILED}.
 *
 * <p>See {@code docs/adr/0003-market-lifecycle-four-states.md} (Q11).
 */
@Service
public class SettlementAdminService {

    private static final Logger log = LoggerFactory.getLogger(SettlementAdminService.class);

    private final PositionSettlementRepository positionSettlementRepository;
    private final MarketRepository marketRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementAdminService(PositionSettlementRepository positionSettlementRepository,
            MarketRepository marketRepository, ApplicationEventPublisher eventPublisher) {
        this.positionSettlementRepository = positionSettlementRepository;
        this.marketRepository = marketRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Retry a {@code SETTLEMENT_FAILED} Market.
     *
     * <p>One transaction:
     * <ol>
     *   <li>Reset every {@code FAILED} row for this Market to {@code PENDING}
     *       with {@code attempts=0}, {@code last_error=null}, {@code next_run_at=now()}.</li>
     *   <li>Flip the Market {@code SETTLEMENT_FAILED -> RESOLUTION_PENDING}.</li>
     * </ol>
     *
     * <p>Both steps are idempotent. Calling on a healthy ({@code RESOLVED},
     * {@code RESOLUTION_PENDING}, {@code OPEN}) Market matches zero rows and
     * publishes an event with {@code resetRowCount=0, marketFlipped=false} —
     * useful for distinguishing "intentional no-op" from "successfully retried."
     *
     * <p>The endpoint enforces ADMIN role; this service does NOT re-check, on
     * the assumption that the security filter has already gated the call.
     */
    @Transactional
    public RetryResult retrySettlement(String marketId) {
        if (marketId == null || marketId.isBlank()) {
            throw new IllegalArgumentException("marketId must not be blank");
        }

        int rowsReset = positionSettlementRepository.resetFailedRowsForRetry(marketId);
        boolean marketFlipped = marketRepository.flipBackToResolutionPending(marketId);

        String actor = currentActorUserId();
        log.info("Settlement retry requested for market={} by actor={} (rowsReset={}, marketFlipped={})",
                marketId, actor, rowsReset, marketFlipped);

        eventPublisher.publishEvent(new MarketSettlementRetryRequestedEvent(
                marketId, actor, rowsReset, marketFlipped));

        return new RetryResult(rowsReset, marketFlipped);
    }

    public record RetryResult(int rowsReset, boolean marketFlipped) {}

    private String currentActorUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
