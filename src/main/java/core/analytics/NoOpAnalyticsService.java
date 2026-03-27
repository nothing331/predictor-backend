package core.analytics;

import java.util.Map;

public class NoOpAnalyticsService implements AnalyticsService {

    @Override
    public void capture(String distinctId, String eventName, Map<String, Object> properties) {
    }

    @Override
    public void identify(String distinctId, Map<String, Object> userProperties) {
    }
}
