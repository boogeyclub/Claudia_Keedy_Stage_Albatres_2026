package cm.odigital.serviceconnectmarket.auth.session;

import java.time.Instant;

/**
 * Safe, user-facing metadata for one connected browser session.
 */
public record BrowserSession(
    long id,
    String browserLabel,
    boolean rememberMe,
    Instant createdAt,
    Instant lastSeenAt,
    Instant expiresAt
) {
}
