package api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import core.analytics.AnalyticsEventNames;
import core.analytics.AnalyticsService;
import core.ratelimit.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AnalyticsService analyticsService;

    public GlobalExceptionHandler(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        maybeCaptureTradeFailure(ex, request, ex.getStatusCode().value());
        ErrorResponse error = new ErrorResponse(
                ex.getStatusCode().value(),
                ex.getStatusCode().toString(),
                ex.getReason() != null ? ex.getReason() : ex.getMessage());
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        maybeCaptureTradeFailure(ex, request, HttpStatus.BAD_REQUEST.value());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        maybeCaptureTradeFailure(ex, request, HttpStatus.BAD_REQUEST.value());
        // Using 400 Bad Request for state issues as well, as it's a client error
        // (invalid action)
        // Could also be 409 Conflict depending on semantics, but 400 is safe.
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        maybeCaptureTradeFailure(ex, request, HttpStatus.BAD_REQUEST.value());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        ErrorResponse error = new ErrorResponse(
                429,
                "Too Many Requests",
                ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void maybeCaptureTradeFailure(Exception ex, HttpServletRequest request, int statusCode) {
        if (request == null || request.getUserPrincipal() == null) {
            return;
        }

        String requestUri = request.getRequestURI();
        if (requestUri == null || !"POST".equalsIgnoreCase(request.getMethod()) || !requestUri.endsWith("/trades")) {
            return;
        }

        analyticsService.capture(request.getUserPrincipal().getName(), AnalyticsEventNames.BET_FAILED, java.util.Map.of(
                "route", requestUri,
                "httpStatus", statusCode,
                "errorType", ex.getClass().getSimpleName(),
                "message", safeMessage(ex),
                "marketId", extractMarketId(requestUri)));
    }

    private String extractMarketId(String requestUri) {
        String[] parts = requestUri.split("/");
        if (parts.length >= 4) {
            return parts[3];
        }
        return "unknown";
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : "Request failed";
    }
}
