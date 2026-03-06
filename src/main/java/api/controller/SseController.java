package api.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import core.event.DomainEvent;

@RestController
@RequestMapping("/v1/stream")
public class SseController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // infinite timeout internally
        this.emitters.add(emitter);

        Runnable onCompletion = () -> this.emitters.remove(emitter);
        emitter.onCompletion(onCompletion);
        emitter.onTimeout(onCompletion);
        emitter.onError(e -> this.emitters.remove(emitter));

        return emitter;
    }

    @EventListener
    public void onDomainEvent(DomainEvent event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
