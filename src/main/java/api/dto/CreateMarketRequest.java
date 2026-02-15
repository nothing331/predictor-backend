package api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import core.market.Market;
import jakarta.validation.constraints.NotBlank;

public class CreateMarketRequest {
    @NotBlank(message = "Market name cannot be empty")
    private String name;

    private String description;

    private double liquidity = 50.0;

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLiquidity(double liquidity) {
        if (liquidity > 0) {
            this.liquidity = liquidity;
        }
    }

    public Market createMarket() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String marketId = name.replaceAll("\\s+", "-") + "-" + timestamp;
        return new Market(marketId, name, description, liquidity);
    }

}
