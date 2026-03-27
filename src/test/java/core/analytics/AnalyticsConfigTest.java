package core.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AnalyticsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AnalyticsConfig.class);

    @Test
    void disabledAnalyticsUsesNoOpService() {
        contextRunner
                .withPropertyValues("app.analytics.posthog.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AnalyticsService.class);
                    assertThat(context.getBean(AnalyticsService.class)).isInstanceOf(NoOpAnalyticsService.class);
                });
    }

    @Test
    void enabledAnalyticsWithApiKeyUsesPostHogService() {
        contextRunner
                .withPropertyValues(
                        "app.analytics.posthog.enabled=true",
                        "app.analytics.posthog.api-key=test-key",
                        "app.analytics.posthog.host=https://eu.i.posthog.com")
                .run(context -> {
                    assertThat(context).hasSingleBean(AnalyticsService.class);
                    assertThat(context.getBean(AnalyticsService.class)).isInstanceOf(PostHogAnalyticsService.class);
                });
    }
}
