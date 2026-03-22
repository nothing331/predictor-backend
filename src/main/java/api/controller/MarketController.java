package api.controller;

import java.time.Instant;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CreateMarketRequest;
import api.dto.GetAllMarket;
import api.dto.MarketHistoryResponse;
import api.dto.ResolveMarketRequest;
import core.service.MarketHistoryService;
import core.service.MarketService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/markets")
public class MarketController {
    @Autowired
    private MarketService marketService;
    @Autowired
    private MarketHistoryService marketHistoryService;

    @PostMapping
    public ResponseEntity<?> createMarket(@Valid @RequestBody CreateMarketRequest request) {
        core.market.Market market = request.createMarket();
        boolean isCreated = marketService.addMarket(market);
        if (!isCreated) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("error", "Market with this name already exists."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(
            java.util.Map.of(
                "status", "success",
                "message", "Market created successfully.",
                "marketId", market.getMarketId()
            )
        );
    }

    @GetMapping
    public ResponseEntity<Collection<GetAllMarket>> getAllMarkets(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(marketService.getAll(status));
    }

    @GetMapping("/{marketId}")
    public ResponseEntity<api.dto.GetAllMarket> getMarketById(@PathVariable String marketId) {
        api.dto.GetAllMarket market = marketService.getMarketById(marketId);
        if (market == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(market);
    }

    @GetMapping("/{marketId}/history")
    public ResponseEntity<MarketHistoryResponse> getMarketHistory(
            @PathVariable String marketId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(marketHistoryService.getHistory(marketId, from, to, limit));
    }

    @PostMapping("/{marketId}/resolve")
    public ResponseEntity<?> resolveMarket(@PathVariable String marketId,
            @RequestBody @Valid ResolveMarketRequest request) {
        marketService.resolveMarket(marketId, request.getOutcomeId());
        return ResponseEntity.ok(
            java.util.Map.of(
                "status", "success",
                "message", "Market resolved successfully.",
                "marketId", marketId,
                "resolvedOutcome", request.getOutcomeId()
            )
        );
    }
}
