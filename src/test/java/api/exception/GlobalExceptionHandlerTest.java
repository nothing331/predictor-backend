package api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import core.analytics.AnalyticsEventNames;
import core.analytics.AnalyticsService;

class GlobalExceptionHandlerTest {

    @Mock
    private AnalyticsService analyticsService;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new GlobalExceptionHandler(analyticsService);
    }

    @Test
    void tradeErrorsAreTrackedAsBetFailures() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/markets/market-1/trades");
        request.setUserPrincipal(() -> "user-1");

        org.springframework.http.ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid outcome: MAYBE"),
                request);

        assertEquals(400, response.getStatusCode().value());
        verify(analyticsService).capture(eq("user-1"), eq(AnalyticsEventNames.BET_FAILED),
                org.mockito.ArgumentMatchers.argThat(props ->
                        "market-1".equals(props.get("marketId"))
                                && Integer.valueOf(400).equals(props.get("httpStatus"))
                                && "Invalid outcome: MAYBE".equals(props.get("message"))));
    }
}
