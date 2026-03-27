package core.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import com.posthog.server.PostHog;
import com.posthog.server.PostHogConfig;

@Configuration
@EnableConfigurationProperties(AnalyticsProperties.class)
public class AnalyticsConfig {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsConfig.class);

    @Bean(destroyMethod = "close")
    public AnalyticsService analyticsService(AnalyticsProperties properties, Environment environment) {
        if (!properties.isEnabled()) {
            return new NoOpAnalyticsService();
        }

        if (!StringUtils.hasText(properties.getApiKey())) {
            logger.warn("PostHog analytics is enabled but no API key is configured. Falling back to no-op.");
            return new NoOpAnalyticsService();
        }

        PostHogConfig config = PostHogConfig.builder(properties.getApiKey())
                .host(properties.getHost())
                .build();

        return new PostHogAnalyticsService(PostHog.with(config), environment);
    }
}
