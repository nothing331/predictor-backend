package core.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ratelimit")
public class RateLimiterProperties {

    private int windowSeconds = 60;
    private boolean failClosed = true;
    private final Trade trade = new Trade();
    private final SseConnect sseConnect = new SseConnect();

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public boolean isFailClosed() {
        return failClosed;
    }

    public void setFailClosed(boolean failClosed) {
        this.failClosed = failClosed;
    }

    public Trade getTrade() {
        return trade;
    }

    public SseConnect getSseConnect() {
        return sseConnect;
    }

    public static class Trade {
        private int max = 30;

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }
    }

    public static class SseConnect {
        private int max = 20;

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }
    }
}
