package api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import api.dto.BuyRequest;
import core.service.TradeService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/markets")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @PostMapping("/{marketId}/trades")
    public ResponseEntity<?> buy(@PathVariable String marketId,
            java.security.Principal principal,
            @RequestBody @Valid BuyRequest request) {
        core.trade.Trade trade = tradeService.buy(request, principal.getName(), marketId);
        return ResponseEntity.ok(
            java.util.Map.of(
                "status", "success",
                "message", "Trade executed successfully.",
                "tradeId", trade.getTradeId(),
                "sharesBought", trade.getShareCount(),
                "cost", trade.getCost(),
                "outcome", trade.getOutcome()
            )
        );
    }

}
