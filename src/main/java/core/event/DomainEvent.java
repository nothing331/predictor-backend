package core.event;

import java.util.Map;

public interface DomainEvent {
    Map<String, Object> getEvent();
}
