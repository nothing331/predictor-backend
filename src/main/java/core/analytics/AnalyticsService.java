package core.analytics;

import java.util.Map;

public interface AnalyticsService extends AutoCloseable {

    void capture(String distinctId, String eventName, Map<String, Object> properties);

    void identify(String distinctId, Map<String, Object> userProperties);

    default void capture(String distinctId, String eventName) {
        capture(distinctId, eventName, Map.of());
    }

    @Override
    default void close() {
    }
}
