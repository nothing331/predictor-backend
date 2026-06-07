package core.market;

/**
 * Lifecycle states for a Market. See docs/adr/0003-market-lifecycle-four-states.md.
 *
 * <p>Allowed transitions:
 * <ul>
 *   <li>{@code OPEN -> RESOLUTION_PENDING} (Resolution endpoint)</li>
 *   <li>{@code RESOLUTION_PENDING -> RESOLVED} (Settlement worker, last row processed)</li>
 *   <li>{@code RESOLUTION_PENDING -> SETTLEMENT_FAILED} (Settlement worker, terminal failure)</li>
 *   <li>{@code SETTLEMENT_FAILED -> RESOLUTION_PENDING} (admin retry-settlement endpoint only)</li>
 * </ul>
 *
 * <p>Invariant enforced by {@link Market#validate()}: any non-{@code OPEN} state has
 * a non-null {@code resolvedOutcome}. The DTO layer keys on {@code resolvedOutcome != null}
 * (not on the specific state) when deciding whether to emit LMSR prices or settled
 * certainties (1.0 / 0.0).
 */
public enum MarketStatus {
    OPEN,
    RESOLUTION_PENDING,
    RESOLVED,
    SETTLEMENT_FAILED
}
