package cm.odigital.serviceconnectmarket.auth.domain;

import java.time.Instant;

public record PendingRegistration(String email, Instant expiresAt) {
}
