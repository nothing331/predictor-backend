package api.controller;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import api.dto.CreateMarketRequest;
import api.dto.GetAllMarket;
import api.dto.ResolveMarketRequest;
import core.service.MarketService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/markets")
public class MarketController {
    @Autowired
    private MarketService marketService;

    @PostMapping
    public ResponseEntity<String> createMarket(@Valid @RequestBody CreateMarketRequest request) {
        boolean isCreated = marketService.addMarket(request.createMarket());
        if (!isCreated) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Market with this name already exists.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Market created successfully.");
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

    @PostMapping("/{marketId}/resolve")
    public ResponseEntity<String> resolveMarket(@PathVariable String marketId,
            @RequestBody @Valid ResolveMarketRequest request) {
        marketService.resolveMarket(marketId, request.getOutcomeId());
        return ResponseEntity.ok("Market resolved successfully.");
    }
}
