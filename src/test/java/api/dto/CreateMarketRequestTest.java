package api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import core.market.Market;

public class CreateMarketRequestTest {

    @Test
    @DisplayName("createMarket maps custom category and labels")
    void createMarketMapsCustomFields() {
        // Arrange
        CreateMarketRequest request = new CreateMarketRequest();
        request.setName("Election Winner");
        request.setDescription("Who will win the election?");
        request.setLiquidity(200.0);
        request.setCategory("Politics");
        request.setYesLabel("Candidate A");
        request.setNoLabel("Candidate B");

        // Act
        Market market = request.createMarket();

        // Assert
        assertEquals("Election Winner", market.getMarketName());
        assertEquals("Who will win the election?", market.getMarketDescription());
        assertEquals(200.0, market.getLiquidity());
        assertEquals("Politics", market.getCategory());
        assertEquals("Candidate A", market.getYesLabel());
        assertEquals("Candidate B", market.getNoLabel());
    }

    @Test
    @DisplayName("createMarket maps default properties if custom ones are null")
    void createMarketMapsDefaultsIfNull() {
        // Arrange
        CreateMarketRequest request = new CreateMarketRequest();
        request.setName("Basic Market");
        request.setDescription("Basic Description");
        // category, yesLabel, noLabel left null

        // Act
        Market market = request.createMarket();

        // Assert
        assertEquals("General", market.getCategory());
        assertEquals("Yes", market.getYesLabel());
        assertEquals("No", market.getNoLabel());
        assertEquals(50.0, market.getLiquidity()); // default liquidity initialized in class
    }
}
