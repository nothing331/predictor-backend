package sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import core.event.DomainEvent;

@Service
public class SseManager {

    private final Map<SseEmitter, String> clients = new ConcurrentHashMap<>();

    public SseEmitter addClient(String marketId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        clients.put(emitter, marketId == null ? "" : marketId);

        Runnable onCompletion = () -> clients.remove(emitter);
        emitter.onCompletion(onCompletion);
        emitter.onTimeout(onCompletion);
        emitter.onError(e -> clients.remove(emitter));

        return emitter;
    }

    public void broadcast(DomainEvent event) {
        Object marketIdObj = event.getEvent().get("marketId");
        String eventMarketId = marketIdObj != null ? marketIdObj.toString() : "";
        Object typeObj = event.getEvent().get("type");
        String eventType = typeObj != null ? typeObj.toString() : "unknown";

        clients.forEach((emitter, filterMarketId) -> {
            boolean matchesFilter = filterMarketId.isEmpty() || filterMarketId.equals(eventMarketId);
            if (matchesFilter) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventType)
                            .data(event.getEvent()));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    clients.remove(emitter);
                }
            }
        });
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeats() {
        clients.keySet().forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                clients.remove(emitter);
            }
        });
    }
}
