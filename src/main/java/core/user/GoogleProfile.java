package core.user;

public record GoogleProfile(
    String googleSub,
    String email,
    String name,
    String pictureUrl,
    boolean emailVerified
) {}
