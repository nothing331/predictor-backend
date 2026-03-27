package api.controller;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

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
import api.dto.UserMarketPositionResponse;
import core.analytics.AnalyticsEventNames;
import core.analytics.AnalyticsService;
import core.service.MarketHistoryService;
import core.service.MarketService;
import core.service.MarketUserPositionService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/markets")
public class MarketController {

    private final MarketService marketService;
    private final MarketHistoryService marketHistoryService;
    private final MarketUserPositionService marketUserPositionService;
    private final AnalyticsService analyticsService;

    public MarketController(MarketService marketService, MarketHistoryService marketHistoryService,
            MarketUserPositionService marketUserPositionService, AnalyticsService analyticsService) {
        this.marketService = marketService;
        this.marketHistoryService = marketHistoryService;
        this.marketUserPositionService = marketUserPositionService;
        this.analyticsService = analyticsService;
    }

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
    public ResponseEntity<Collection<GetAllMarket>> getAllMarkets(@RequestParam(required = false) String status,
            java.security.Principal principal) {
        Collection<GetAllMarket> markets = marketService.getAll(status);
        if (principal != null) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("resultCount", markets.size());
            if (status != null && !status.isBlank()) {
                properties.put("statusFilter", status);
            }
            analyticsService.capture(principal.getName(), AnalyticsEventNames.MARKET_LIST_VIEWED, properties);
        }
        return ResponseEntity.ok(markets);
    }

    @GetMapping("/{marketId}")
    public ResponseEntity<api.dto.GetAllMarket> getMarketById(@PathVariable String marketId,
            java.security.Principal principal) {
        api.dto.GetAllMarket market = marketService.getMarketById(marketId);
        if (market == null) {
            if (principal != null) {
                analyticsService.capture(principal.getName(), AnalyticsEventNames.MARKET_VIEW_FAILED, Map.of(
                        "marketId", marketId,
                        "httpStatus", HttpStatus.NOT_FOUND.value(),
                        "route", "/v1/markets/" + marketId,
                        "errorType", "NotFound",
                        "message", "Market not found"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (principal != null) {
            Map<String, Object> properties = new LinkedHashMap<>();
            properties.put("marketId", marketId);
            properties.put("marketStatus", market.getStatus().toString());
            if (market.getCategory() != null) {
                properties.put("category", market.getCategory());
            }
            if (market.getResolvedOutcome() != null) {
                properties.put("resolvedOutcome", market.getResolvedOutcome().toString());
            }
            analyticsService.capture(principal.getName(), AnalyticsEventNames.MARKET_VIEWED, properties);
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

    @GetMapping("/{marketId}/me")
    public ResponseEntity<UserMarketPositionResponse> getMarketPosition(@PathVariable String marketId,
            java.security.Principal principal) {
        return ResponseEntity.ok(marketUserPositionService.getMarketPosition(principal.getName(), marketId));
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
