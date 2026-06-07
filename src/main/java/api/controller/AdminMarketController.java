package api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import core.market.MarketStatus;
import core.service.SettlementAdminService;
import core.service.SettlementAdminService.RetryResult;

/**
 * Admin-only endpoints for the settlement pipeline.
 *
 * <p>Gated by {@code hasRole("ADMIN")} in {@code SecurityConfig}; the
 * {@code /v1/admin/**} prefix is the security boundary.
 */
@RestController
@RequestMapping("/v1/admin/markets")
public class AdminMarketController {

    private final SettlementAdminService settlementAdminService;

    public AdminMarketController(SettlementAdminService settlementAdminService) {
        this.settlementAdminService = settlementAdminService;
    }

    /**
     * Retry a {@code SETTLEMENT_FAILED} Market: reset failed rows to
     * {@code PENDING} and flip the Market back to {@code RESOLUTION_PENDING}.
     * The Settlement Worker picks the rows up on its next tick.
     *
     * <p>Returns {@code 202 Accepted} — same async-completion contract as
     * resolve. Body indicates how many rows were reset and whether the Market
     * itself transitioned.
     *
     * <p>Idempotent. Calling on a healthy Market returns 202 with
     * {@code rowsReset=0, marketFlipped=false}.
     *
     * <p>See {@code docs/adr/0003-market-lifecycle-four-states.md}.
     */
    @PostMapping("/{marketId}/retry-settlement")
    public ResponseEntity<?> retrySettlement(@PathVariable String marketId) {
        RetryResult result = settlementAdminService.retrySettlement(marketId);
        return ResponseEntity
                .accepted()
                .header("Location", "/v1/markets/" + marketId)
                .body(java.util.Map.of(
                        "status", MarketStatus.RESOLUTION_PENDING.toString(),
                        "marketId", marketId,
                        "rowsReset", result.rowsReset(),
                        "marketFlipped", result.marketFlipped(),
                        "message", "Retry enqueued. Settlement worker will pick up failed rows."));
    }
}
