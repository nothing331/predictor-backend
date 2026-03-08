package api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import core.event.DomainEvent;
import core.ratelimit.RateLimiterService;
import sse.SseManager;

public class SseControllerTest {

    private SseManager sseManager;
    private RateLimiterService rateLimiterService;
    private SseController sseController;

    @BeforeEach
    public void setup() {
        sseManager = mock(SseManager.class);
        rateLimiterService = mock(RateLimiterService.class);
        sseController = new SseController(sseManager, rateLimiterService);
    }

    @Test
    public void testStream() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        SseEmitter mockEmitter = mock(SseEmitter.class);

        when(sseManager.addClient("testMarket")).thenReturn(mockEmitter);

        SseEmitter result = sseController.stream("testMarket", "user123", request);

        verify(rateLimiterService).guardSseConnect("user123", "10.0.0.2");
        verify(sseManager).addClient("testMarket");

        assertEquals(mockEmitter, result);
    }

    @Test
    public void testStreamWithXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.168.0.1, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1");
        SseEmitter mockEmitter = mock(SseEmitter.class);

        when(sseManager.addClient("testMarket")).thenReturn(mockEmitter);

        SseEmitter result = sseController.stream("testMarket", null, request);

        verify(rateLimiterService).guardSseConnect(null, "192.168.0.1");
        verify(sseManager).addClient("testMarket");

        assertEquals(mockEmitter, result);
    }

    @Test
    public void testOnDomainEvent() {
        DomainEvent event = () -> Map.of("marketId", "market1", "type", "market.created");

        sseController.onDomainEvent(event);

        verify(sseManager).broadcast(event);
    }
}
