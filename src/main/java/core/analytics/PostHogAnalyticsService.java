package core.analytics;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.posthog.server.PostHogCaptureOptions;
import com.posthog.server.PostHogInterface;

import jakarta.servlet.http.HttpServletRequest;

public class PostHogAnalyticsService implements AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(PostHogAnalyticsService.class);

    private final PostHogInterface postHog;
    private final Environment environment;

    public PostHogAnalyticsService(PostHogInterface postHog, Environment environment) {
        this.postHog = postHog;
        this.environment = environment;
    }

    @Override
    public void capture(String distinctId, String eventName, Map<String, Object> properties) {
        if (!StringUtils.hasText(distinctId) || !StringUtils.hasText(eventName)) {
            return;
        }

        try {
            PostHogCaptureOptions options = PostHogCaptureOptions.builder()
                    .properties(enrichProperties(distinctId, properties))
                    .timestamp(Instant.now())
                    .build();

            postHog.capture(distinctId, eventName, options);
        } catch (Exception exception) {
            logger.warn("Failed to capture analytics event {}", eventName, exception);
        }
    }

    @Override
    public void identify(String distinctId, Map<String, Object> userProperties) {
        if (!StringUtils.hasText(distinctId)) {
            return;
        }

        try {
            postHog.identify(distinctId, sanitize(userProperties));
        } catch (Exception exception) {
            logger.warn("Failed to identify analytics user {}", distinctId, exception);
        }
    }

    @Override
    public void close() {
        try {
            postHog.flush();
        } catch (Exception exception) {
            logger.warn("Failed to flush PostHog analytics events", exception);
        }

        try {
            postHog.close();
        } catch (Exception exception) {
            logger.warn("Failed to close PostHog analytics client", exception);
        }
    }

    private Map<String, Object> enrichProperties(String distinctId, Map<String, Object> properties) {
        Map<String, Object> enrichedProperties = new LinkedHashMap<>(sanitize(properties));
        enrichedProperties.putIfAbsent("userId", distinctId);
        enrichedProperties.putIfAbsent("environment", resolveEnvironment());

        HttpServletRequest request = currentRequest();
        if (request != null) {
            enrichedProperties.putIfAbsent("requestPath", request.getRequestURI());
            enrichedProperties.putIfAbsent("requestMethod", request.getMethod());
        }

        return enrichedProperties;
    }

    private Map<String, Object> sanitize(Map<String, Object> input) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (input == null) {
            return sanitized;
        }

        input.forEach((key, value) -> {
            if (StringUtils.hasText(key) && Objects.nonNull(value)) {
                sanitized.put(key, value);
            }
        });

        return sanitized;
    }

    private String resolveEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return "default";
        }
        return String.join(",", Arrays.asList(activeProfiles));
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
