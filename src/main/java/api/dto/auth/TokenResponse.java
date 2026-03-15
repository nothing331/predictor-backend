package api.dto.auth;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    long expiresInSeconds
) {}
