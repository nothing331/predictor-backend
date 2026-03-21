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

    private String category;

    private String yesLabel;

    private String noLabel;

    public void setCategory(String category) {
        this.category = category;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setYesLabel(String yesLabel) {
        this.yesLabel = yesLabel;
    }

    public void setNoLabel(String noLabel) {
        this.noLabel = noLabel;
    }

    public void setLiquidity(double liquidity) {
        if (liquidity > 0) {
            this.liquidity = liquidity;
        }
    }

    public Market createMarket() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String marketId = name.replaceAll("\\s+", "-") + "-" + timestamp;
        return new Market(marketId, name, description, liquidity, category, yesLabel, noLabel);
    }

}
