package api.dto;

import jakarta.validation.constraints.NotBlank;

public class ResolveMarketRequest {
    @NotBlank(message = "outcome cannot be empty")
    private String outcomeId;

    public String getOutcomeId() {
        return outcomeId;
    }

    public void setOutcomeId(String outcomeId) {
        this.outcomeId = outcomeId;
    }

}
