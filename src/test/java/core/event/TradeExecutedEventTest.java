package core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class TradeExecutedEventTest {

    @Test
    public void enrichedConstructor_includesGraphFields() {
        TradeExecutedEvent event = new TradeExecutedEvent(
                "trade-1",
                "market-1",
                "user-1",
                "YES",
                10.5,
                new BigDecimal("25.40"),
                0.62,
                0.38,
                42.5,
                26.0,
                "OPEN");

        assertEquals(0.62, event.payload().get("yesProbability"));
        assertEquals(0.38, event.payload().get("noProbability"));
        assertEquals(42.5, event.payload().get("qYes"));
        assertEquals(26.0, event.payload().get("qNo"));
        assertEquals("OPEN", event.payload().get("status"));
    }
}
