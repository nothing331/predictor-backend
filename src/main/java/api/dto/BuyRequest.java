package api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BuyRequest {

    @NotBlank(message = "Outcome cannot be empty")
    private String outcome;

    @NotNull(message = "Amount cannot be null")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Double amount;

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        if (outcome.trim().equalsIgnoreCase("yes") || outcome.trim().equalsIgnoreCase("no")) {
            this.outcome = outcome;
        } else {
            throw new IllegalArgumentException("Invalid outcome");
        }
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
