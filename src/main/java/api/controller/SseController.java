package api.controller;

import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import core.event.DomainEvent;
import sse.SseManager;

@RestController
@RequestMapping("/v1/stream")
public class SseController {

    private final SseManager sseManager;

    public SseController(SseManager sseManager) {
        this.sseManager = sseManager;
    }

    /**
     * Client recovery rule:
     * On reconnect, client calls REST snapshot endpoints first, then opens stream.
     */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String marketId) {
        return sseManager.addClient(marketId);
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        sseManager.broadcast(event);
    }
}
