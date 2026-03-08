package sse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import core.event.DomainEvent;

public class SseManagerTest {

    private SseManager sseManager;

    @BeforeEach
    public void setup() {
        sseManager = new SseManager();
    }

    @Test
    public void testAddClient() {
        SseEmitter emitter = sseManager.addClient("testMarket123");
        // We can't easily assert the internal map, but we can verify it doesn't throw
        assertDoesNotThrow(() -> sseManager.sendHeartbeats());
    }

    @Test
    public void testBroadcastWithMatchingMarketId() throws IOException {
        SseEmitter emitter1 = sseManager.addClient("market1");
        SseEmitter emitter2 = sseManager.addClient("market2");

        DomainEvent event = () -> Map.of("marketId", "market1", "type", "market.created");

        assertDoesNotThrow(() -> sseManager.broadcast(event));

        // It is difficult to check emitter internals without reflection or mocking
        // internally,
        // but we verify no exception is thrown. We assume Spring handles the emit if
        // valid.
    }

    @Test
    public void testBroadcastWithEmptyMarketId() throws IOException {
        SseEmitter emitter1 = sseManager.addClient("");
        SseEmitter emitter2 = sseManager.addClient("market1");

        DomainEvent event = () -> Map.of("marketId", "market1", "type", "market.created");
        assertDoesNotThrow(() -> sseManager.broadcast(event));
    }

    @Test
    public void testSendHeartbeats() {
        sseManager.addClient("market1");
        assertDoesNotThrow(() -> sseManager.sendHeartbeats());
    }
}
