package core.market;

public class MarketStats {
    private Outcome outcomeId;
    private String label;
    private double probability;

    public MarketStats(Outcome outcomeId, String label, double probability) {
        this.outcomeId = outcomeId;
        this.label = label;
        this.probability = probability;
    }

    /** Default constructor for frameworks (e.g., Jackson). */
    protected MarketStats() {
    }

    public Outcome getOutcomeId() {
        return outcomeId;
    }

    public String getLabel() {
        return label;
    }

    public double getProbability() {
        return probability;
    }
}
