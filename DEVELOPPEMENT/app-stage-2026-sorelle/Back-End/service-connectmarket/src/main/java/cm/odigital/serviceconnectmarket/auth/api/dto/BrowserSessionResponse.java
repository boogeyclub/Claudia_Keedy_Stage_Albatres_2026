package cm.odigital.serviceconnectmarket.auth.api.dto;

import java.time.Instant;

public record BrowserSessionResponse(
    long id,
    String browserLabel,
    boolean rememberMe,
    Instant createdAt,
    Instant lastSeenAt,
    Instant expiresAt,
    boolean current
) {
}
