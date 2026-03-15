package api.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import core.ratelimit.RateLimitExceededException;
import core.ratelimit.RateLimiterService;
import jakarta.servlet.ServletException;

public class RateLimitFilterTest {

    private RateLimiterService rateLimiterService;
    private RateLimitFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup() {
        rateLimiterService = mock(RateLimiterService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        filter = new RateLimitFilter(rateLimiterService, objectMapper);
    }

    @Test
    public void testProtectedEndpointPost() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/markets/123/trades");
        request.setMethod("POST");
        request.addHeader("userId", "testUser");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).guard(request, "testUser", "127.0.0.1");
    }

    @Test
    public void testProtectedEndpointPut() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/markets/123");
        request.setMethod("PUT");
        request.addHeader("userId", "testUser");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).guard(request, "testUser", "127.0.0.1");
    }

    @Test
    public void testProtectedEndpointDelete() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/markets/123");
        request.setMethod("DELETE");
        request.setRemoteAddr("10.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).guard(request, null, "10.0.0.1");
    }

    @Test
    public void testUnprotectedEndpointGet() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/markets");
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService, never()).guard(any(), anyString(), anyString());
    }

    @Test
    public void testIpExtractionFromXForwardedFor() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/markets/123/trades");
        request.setMethod("POST");
        request.addHeader("X-Forwarded-For", "192.168.1.1, 10.0.0.1");
        request.setRemoteAddr("127.0.0.1"); // Should be ignored

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiterService).guard(request, null, "192.168.1.1");
    }

    @Test
    public void testRateLimitExceeded() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/markets/123/trades");
        request.setMethod("POST");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = mock(MockFilterChain.class);

        doThrow(new RateLimitExceededException("Limit Exceeded")).when(rateLimiterService)
                .guard(any(), any(), any());

        filter.doFilter(request, response, filterChain);

        // Verify filterChain.doFilter was not called
        verify(filterChain, never()).doFilter(request, response);

        assertEquals(429, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("\"status\": 429"));
        assertTrue(response.getContentAsString().contains("Limit Exceeded"));
    }
}
