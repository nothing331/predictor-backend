package core.event;

/**
 * Published when an admin requests retry of a {@code SETTLEMENT_FAILED} Market
 * via {@code POST /v1/admin/markets/{id}/retry-settlement}.
 *
 * @param marketId        the affected Market
 * @param actorUserId     the admin user who triggered the retry (audit trail)
 * @param resetRowCount   how many {@code position_settlements} rows transitioned
 *                        from {@code FAILED} back to {@code PENDING}
 * @param marketFlipped   true if the Market also transitioned
 *                        {@code SETTLEMENT_FAILED -> RESOLUTION_PENDING}
 */
public record MarketSettlementRetryRequestedEvent(
        String marketId,
        String actorUserId,
        int resetRowCount,
        boolean marketFlipped) {
}
