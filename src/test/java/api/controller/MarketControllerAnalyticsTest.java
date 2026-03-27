package api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import api.dto.GetAllMarket;
import core.analytics.AnalyticsEventNames;
import core.analytics.AnalyticsService;
import core.market.MarketStatus;
import core.service.MarketHistoryService;
import core.service.MarketService;
import core.service.MarketUserPositionService;

class MarketControllerAnalyticsTest {

    @Mock
    private MarketService marketService;

    @Mock
    private MarketHistoryService marketHistoryService;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private MarketUserPositionService marketUserPositionService;

    private MarketController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new MarketController(
                marketService,
                marketHistoryService,
                marketUserPositionService,
                analyticsService);
    }

    @Test
    void authenticatedMarketListViewIsTracked() {
        when(marketService.getAll("OPEN")).thenReturn(List.of());

        ResponseEntity<java.util.Collection<GetAllMarket>> response =
                controller.getAllMarkets("OPEN", principal("user-1"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<Map<String, Object>> propertiesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(analyticsService).capture(eq("user-1"), eq(AnalyticsEventNames.MARKET_LIST_VIEWED),
                propertiesCaptor.capture());
        assertEquals(0, propertiesCaptor.getValue().get("resultCount"));
        assertEquals("OPEN", propertiesCaptor.getValue().get("statusFilter"));
    }

    @Test
    void missingMarketTracksMarketViewFailure() {
        when(marketService.getMarketById("market-1")).thenReturn(null);

        ResponseEntity<GetAllMarket> response = controller.getMarketById("market-1", principal("user-1"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(analyticsService).capture(eq("user-1"), eq(AnalyticsEventNames.MARKET_VIEW_FAILED),
                org.mockito.ArgumentMatchers.argThat(props ->
                        "market-1".equals(props.get("marketId"))
                                && Integer.valueOf(404).equals(props.get("httpStatus"))));
    }

    @Test
    void existingMarketTracksMarketViewSuccess() {
        GetAllMarket market = new GetAllMarket(
                "market-1",
                "Will it rain?",
                MarketStatus.OPEN,
                null,
                "Weather",
                List.of(),
                BigDecimal.ZERO);
        when(marketService.getMarketById("market-1")).thenReturn(market);

        ResponseEntity<GetAllMarket> response = controller.getMarketById("market-1", principal("user-1"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(analyticsService).capture(eq("user-1"), eq(AnalyticsEventNames.MARKET_VIEWED),
                org.mockito.ArgumentMatchers.argThat(props ->
                        "market-1".equals(props.get("marketId"))
                                && "OPEN".equals(props.get("marketStatus"))));
    }

    private Principal principal(String name) {
        return () -> name;
    }
}
