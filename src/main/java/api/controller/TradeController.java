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
    public ResponseEntity<String> buy(@PathVariable String marketId,
            @RequestHeader String userId,
            @RequestBody @Valid BuyRequest request) {
        tradeService.buy(request, userId, marketId);
        return ResponseEntity.ok("Trade executed successfully.");
    }

}
