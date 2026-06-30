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

    // Optional client-supplied idempotency key. When absent, the server generates
    // one in TradeService.buy(); note a client that omits it gets no retry-dedup
    // guarantee, since each retry without an id is treated as a distinct request.
    private String clientRequestId;

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

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }
}
