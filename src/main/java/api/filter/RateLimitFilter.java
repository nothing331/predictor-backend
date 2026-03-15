package api.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import core.ratelimit.RateLimitExceededException;
import core.ratelimit.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            if (isProtected(request)) {
                String userId = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : request.getHeader("userId");
                String ip = getClientIp(request);
                rateLimiterService.guard(request, userId, ip);
            }
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException ex) {
            response.setStatus(429);
            response.setContentType("application/json");
            api.exception.ErrorResponse errorResponse = new api.exception.ErrorResponse(
                    429,
                    "Too Many Requests",
                    ex.getMessage()
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private boolean isProtected(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Protect mutation endpoints for /v1/markets
        if (uri.startsWith("/v1/markets") && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method))) {
            return true;
        }
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
