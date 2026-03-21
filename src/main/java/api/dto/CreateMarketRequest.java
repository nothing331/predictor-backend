package api.dto;

import core.market.Market;
import jakarta.validation.constraints.NotBlank;

public class CreateMarketRequest {
    @NotBlank(message = "Market name cannot be empty")
    private String name;

    private String description;

    private double liquidity = 35.35;

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
        return new Market(java.util.UUID.randomUUID().toString(), name, description, liquidity, category, yesLabel, noLabel);
    }

}
