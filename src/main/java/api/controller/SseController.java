package api.controller;

import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import core.event.DomainEvent;
import core.ratelimit.RateLimiterService;
import sse.SseManager;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/stream")
public class SseController {

    private final SseManager sseManager;
    private final RateLimiterService rateLimiterService;

    public SseController(SseManager sseManager, RateLimiterService rateLimiterService) {
        this.sseManager = sseManager;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Client recovery rule:
     * On reconnect, client calls REST snapshot endpoints first, then opens stream.
     */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String marketId,
            java.security.Principal principal,
            HttpServletRequest request) {
        String userId = principal != null ? principal.getName() : null;
        String ip = getClientIp(request);
        rateLimiterService.guardSseConnect(userId, ip);
        return sseManager.addClient(marketId);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        sseManager.broadcast(event);
    }
}
